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
				val currentCoord = Coordinate(event.x, event.y)
				val now = java.time.Instant.now()
				val idleTimeMillis = now.toEpochMilli() - state.lastUpdateTime.toEpochMilli()

				// 1. 타임아웃(30초) 또는 시작점 복귀 검출 시 초기화
				val isTimeout = idleTimeMillis > 30000 // 30초 이상 활동 없으면 새 세션으로 간주
				val isReturnToStart = false // Dynamic start point; no fixed reset zone

				if (isTimeout || isReturnToStart) {
					val reason = if (isTimeout) "Inactivity timeout ($idleTimeMillis ms)" else "Return to start (x=${event.x})"
					log.info("Resetting session ${state.sessionId} to Step 1. Reason: $reason")
					state.currentStep = 1
					state.isCompleted = false
					state.collisionDetected = false
					state.trajectory.clear()
					state.maxAbsHandleAngleInStep = 0.0
					state.startTime = now
					state.initialX = null
					state.initialY = null
					state.stabilityStartSimTime = null
				}
				state.lastUpdateTime = now

				// Step 1 진입 시 첫 좌표를 기준점으로 캡처
				if (state.currentStep == 1 && state.initialX == null) {
					state.initialX = event.x
					state.initialY = event.y
					log.info("Captured initial position for session ${state.sessionId}: (${state.initialX}, ${state.initialY})")
				}

				// 2. 실시간 충돌 체크 (거리 센서 기반)
				if (checkCollision(event)) {
					state.collisionDetected = true
					return@flatMap stateRepository.save(state).map {
						createDefeatingResult(state, "Collision detected (distance < ${ParkingReference.COLLISION_DISTANCE_THRESHOLD}m)")
					}
				}

				// 3. 상태 업데이트 (Trajectory, Max Handle 보관)
				state.updateWith(currentCoord, event.handleAngle)

				// 4. Step 종료 판별 로직
				// (CSV 데이터 기반 정교화: 목표 도달 + 1초(1000ms) 이상 정지 상태 확인)
				val isGoalReached = when (state.currentStep) {
					1 -> event.x >= ParkingReference.STEP_1.x - ParkingReference.GOAL_REACHED_POSITION_TOLERANCE_M
					2 -> state.maxAbsHandleAngleInStep >= ParkingReference.GOAL_REACHED_HANDLE_ANGLE_THRESHOLD_DEG && 
						 Math.abs(event.handleAngle) < ParkingReference.STABLE_HANDLE_ANGLE_TOLERANCE_DEG && 
						 kotlin.math.hypot(event.x - ParkingReference.STEP_2.x, event.y - ParkingReference.STEP_2.y) < ParkingReference.GOAL_REACHED_POSITION_TOLERANCE_M
					3 -> state.maxAbsHandleAngleInStep >= ParkingReference.GOAL_REACHED_HANDLE_ANGLE_THRESHOLD_DEG && 
						 Math.abs(event.handleAngle) < ParkingReference.STABLE_HANDLE_ANGLE_TOLERANCE_DEG && 
						 kotlin.math.hypot(event.x - ParkingReference.STEP_3.x, event.y - ParkingReference.STEP_3.y) < ParkingReference.GOAL_REACHED_POSITION_TOLERANCE_M
					else -> false
				}

				val isSpeedStable = Math.abs(event.sensor.speed) < ParkingReference.STABLE_SPEED_THRESHOLD_MPS
				
				val stabilityStartTime = state.stabilityStartSimTime
				// 리스타트 판별 (이전 상태의 stabilityStartSimTime이 있을 때 시간이 역행하면 리스타트로 간주)
				val isRestart = stabilityStartTime != null && event.time < stabilityStartTime

				if (isGoalReached && isSpeedStable) {
					if (stabilityStartTime == null) {
						state.stabilityStartSimTime = event.time
						log.info("Step ${state.currentStep} goal reached and stable. Starting stability timer at sim time ${event.time}")
					}
				} else if (!isRestart) { 
					// 리스타트 상황이 아닐 때만 목표 미달 시 타이머 초기화
					state.stabilityStartSimTime = null
				}

				val currentStabilityStartTime = state.stabilityStartSimTime
				val stabilityThreshold = if (state.currentStep == 1) 1.0 else 3.0
				val isStepEnd = (currentStabilityStartTime != null && (event.time - currentStabilityStartTime) >= stabilityThreshold) || isRestart

				if (isStepEnd) {
					log.info("Step ${state.currentStep} COMPLETED. Advancing to next step. (isRestart=$isRestart, x=${event.x})")
					val result = evaluateStep(state, event) // 현재 스텝에 대한 최종 결과(완료)
					
					state.advanceToNextStep()
					state.stabilityStartSimTime = null // 다음 스텝을 위해 초기화

					return@flatMap stateRepository.save(state).map { 
						if (isRestart) {
							// 리스타트의 경우, 현재 레코드가 이미 다음 스텝의 시작이어야 하므로 다음 스텝 결과 반환
							evaluateStep(state, event)
						} else {
							result 
						}
					}
				}
 else {
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


		// 4. 오차 평가 (감점 로직 제거, 수치만 제공)
		val msg = StringBuilder("Step ${state.currentStep} Evaluated.")
		if (errorX > ParkingReference.TOLERANCE_X) { msg.append(" [X Diff: $errorX]") }
		if (errorY > ParkingReference.TOLERANCE_Y) { msg.append(" [Y Diff: $errorY]") }
		if (errorZ > ParkingReference.TOLERANCE_Z) { msg.append(" [Z Diff: $errorZ]") }
		if (errorHandle > ParkingReference.TOLERANCE_HANDLE) { msg.append(" [Handle Diff: $errorHandle]") }
		if (errorYaw != null && errorYaw > ParkingReference.TOLERANCE_YAW) { msg.append(" [Yaw Diff: $errorYaw]") }

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
			initialX = state.initialX,
			initialY = state.initialY,
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
		initialX = state.initialX,
		initialY = state.initialY,
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
		initialX = state.initialX,
		initialY = state.initialY,
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
		initialX = state.initialX,
		initialY = state.initialY,
		message = message
	)
}
