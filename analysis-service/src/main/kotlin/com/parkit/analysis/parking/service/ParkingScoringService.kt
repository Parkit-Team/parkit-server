package com.parkit.analysis.parking.service

import com.parkit.analysis.parking.domain.Coordinate
import com.parkit.analysis.parking.domain.ParkingEvent
import com.parkit.analysis.parking.domain.ParkingReference
import com.parkit.analysis.parking.domain.ParkingStepState
import com.parkit.analysis.parking.dto.ScoringResultDto
import com.parkit.analysis.parking.repository.ParkingStepStateRepository
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import reactor.core.publisher.Mono
import kotlin.math.atan2
import kotlin.math.pow
import kotlin.math.sqrt

@Service
class ParkingScoringService(
    private val stateRepository: ParkingStepStateRepository
) {
    private val log = LoggerFactory.getLogger(javaClass)

    /**
     * 차량 좌표 스트림 수신 시 호출되는 Main 로직 구동부
     */
	fun processParkingEvent(sessionId: String, event: ParkingEvent): Mono<ScoringResultDto> {
        return stateRepository.findById(sessionId)
            .switchIfEmpty(
                Mono.defer {
                    val newState = ParkingStepState(sessionId = sessionId)
                    stateRepository.save(newState).thenReturn(newState)
                }
            )
            .flatMap { state ->
                // 1. 이미 실격 또는 종료된 세션이면 처리 무시
                if (state.isCompleted || state.collisionDetected) {
                    return@flatMap Mono.just(createIgnoredResult(state, "Session already completed or failed."))
                }

                val currentCoord = Coordinate(event.x, event.y)

                // 2. 실시간 충돌 체크 (거리 센서 기반)
                if (checkCollision(event)) {
                    state.collisionDetected = true
                    state.totalScore -= 100.0 // 실격
                    return@flatMap stateRepository.save(state).map {
                        createDefeatingResult(state, "Collision detected (distance < ${ParkingReference.COLLISION_DISTANCE_THRESHOLD}m)")
                    }
                }

                // 3. 상태 업데이트 (Trajectory, Max Handle 보관)
                state.updateWith(currentCoord, event.handleAngle)

                // 4. Step 종료 판별 로직
                val ref = ParkingReference.getReferenceForStep(state.currentStep)
                val isStepEnd = when (state.currentStep) {
                    1 -> ref != null && event.x >= ref.x - 0.5
                    2, 3 -> state.maxAbsHandleAngleInStep >= 540.0 && Math.abs(event.handleAngle) < 1.0
                    else -> false
                }

                if (isStepEnd) {
                    // 5. Step 평가 및 점수 산정
                    val result = evaluateStep(state, event)

                    // 6. Step 진행
                    state.advanceToNextStep()

                    return@flatMap stateRepository.save(state).map { result }
                } else {
                    // Step 진행 중
                    return@flatMap stateRepository.save(state).map {
                        createInProgressResult(state, "Step ${state.currentStep} in progress...")
                    }
                }
            }
    }

	private fun checkCollision(event: ParkingEvent): Boolean {
        val threshold = ParkingReference.COLLISION_DISTANCE_THRESHOLD
        return event.sensor.frontDistance < threshold ||
               event.sensor.rearDistance < threshold ||
               event.sensor.leftDistance < threshold ||
               event.sensor.rightDistance < threshold
    }

	private fun evaluateStep(state: ParkingStepState, currentEvent: ParkingEvent): ScoringResultDto {
        val ref = ParkingReference.getReferenceForStep(state.currentStep)
        if (ref == null) {
            log.warn("Unknown step: ${state.currentStep}")
            return createIgnoredResult(state, "Unknown step")
        }

        // 1. 오차 계산 (x, y, z, handle)
        val errorX = Math.abs(currentEvent.x - ref.x)
        val errorY = Math.abs(currentEvent.y - ref.y)
        val errorZ = Math.abs(currentEvent.z - ref.z)
        val errorHandle = Math.abs(currentEvent.handleAngle - ref.handleAngle)

        // 2. Yaw (헤딩) 동적 계산 및 오차
        // Trajectory의 맨 처음과 끝 점 간의 yaw를 구하거나, ref.targetYaw와 비교
        val errorYaw = computeYawError(state, ref.targetYaw)

        // 3. Trajectory 유사도 계산 (여기서는 단순히 시작점~끝점 간 유클리드 거리 MSE 가정)
        // 실제로는 DTW 또는 더 복잡한 알고리즘이 필요.
        val trajectoryScore = computeTrajectoryMse(state, ref.x, ref.y)

        // 4. 감점 결정 로직
        var deduction = 0.0
        val msg = StringBuilder("Step ${state.currentStep} Evaluated.")
        if (errorX > ParkingReference.TOLERANCE_X) { deduction += 5.0; msg.append(" [X Diff: $errorX]") }
        if (errorY > ParkingReference.TOLERANCE_Y) { deduction += 5.0; msg.append(" [Y Diff: $errorY]") }
        if (errorZ > ParkingReference.TOLERANCE_Z) { deduction += 2.0; msg.append(" [Z Diff: $errorZ]") }
        if (errorHandle > ParkingReference.TOLERANCE_HANDLE) { deduction += 5.0; msg.append(" [Handle Diff: $errorHandle]") }

        if (errorYaw != null && errorYaw > ParkingReference.TOLERANCE_YAW) { deduction += 5.0; msg.append(" [Yaw Diff: $errorYaw]") }

        state.totalScore -= deduction

        return ScoringResultDto(
            sessionId = state.sessionId,
            step = state.currentStep,
            isCompleted = state.currentStep >= 4, // 4단계까지 오면 완료
            isCollision = false,
            errorX = errorX,
            errorY = errorY,
            errorZ = errorZ,
            errorHandle = errorHandle,
            errorYaw = errorYaw,
            trajectorySimilarityScore = trajectoryScore,
            scoreDeduction = deduction,
            message = msg.toString()
        )
    }

    private fun computeYawError(state: ParkingStepState, targetYaw: Double?): Double? {
        if (targetYaw == null || state.trajectory.size < 2) return null

        val startPoint = state.trajectory.first()
        val endPoint = state.trajectory.last()

        // dy, dx 계산하여 각도(도) 산출
        val dy = endPoint.y - startPoint.y
        val dx = endPoint.x - startPoint.x
        var currentYaw = Math.toDegrees(atan2(dy, dx))

        // Normalize -180 ~ 180
        if (currentYaw < -180.0) currentYaw += 360.0
        if (currentYaw > 180.0) currentYaw -= 360.0

        return Math.abs(currentYaw - targetYaw)
    }

    private fun computeTrajectoryMse(state: ParkingStepState, targetX: Double, targetY: Double): Double {
        if (state.trajectory.isEmpty()) return 0.0
        // 여기서는 가장 단순하게 마지막 좌표와 목표 좌표간의 유클리드 직선 거리를
        // 궤적 점수로 변환 (추후 고도화 가능 지점)
        val lastPoint = state.trajectory.last()
        val dist = sqrt((lastPoint.x - targetX).pow(2) + (lastPoint.y - targetY).pow(2))
        return dist
    }

    private fun createDefeatingResult(state: ParkingStepState, message: String) = ScoringResultDto(
        sessionId = state.sessionId,
        step = state.currentStep,
        isCompleted = true,   // 실격 시 더이상 진행 못함
        isCollision = true,
        errorX = null,
        errorY = null,
        errorZ = null,
        errorHandle = null,
        errorYaw = null,
        trajectorySimilarityScore = null,
        scoreDeduction = 100.0,
        message = message
    )

    private fun createIgnoredResult(state: ParkingStepState, message: String) = ScoringResultDto(
        sessionId = state.sessionId,
        step = state.currentStep,
        isCompleted = state.isCompleted,
        isCollision = state.collisionDetected,
        errorX = null,
        errorY = null,
        errorZ = null,
        errorHandle = null,
        errorYaw = null,
        trajectorySimilarityScore = null,
        scoreDeduction = 0.0,
        message = message
    )

    private fun createInProgressResult(state: ParkingStepState, message: String) = ScoringResultDto(
        sessionId = state.sessionId,
        step = state.currentStep,
        isCompleted = false,
        isCollision = false,
        errorX = null,
        errorY = null,
        errorZ = null,
        errorHandle = null,
        errorYaw = null,
        trajectorySimilarityScore = null,
        scoreDeduction = 0.0,
        message = message
    )
}
