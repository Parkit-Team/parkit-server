package com.parkit.analysis.coaching.service

import com.parkit.analysis.coaching.dto.CoachingSocketDto
import com.parkit.analysis.coaching.dto.ObstacleDistancesDto
import com.parkit.analysis.kafka.dto.ParkingSensorDto
import com.parkit.analysis.parking.domain.ParkingReference
import org.springframework.stereotype.Service
import kotlin.math.roundToInt

@Service
class RiskDetectionService(
) {
    /**
     * 주차 센서와 장애물 간의 거리
     */
    companion object {
		private const val DANGER_LIMIT_FRONT_BACK = 200
		private const val DANGER_LIMIT_SIDE = 80
    }

    /**
     * 주행 이벤트를 기반으로 코칭 이벤트를 계산합니다.
     */
	fun calculate(step: Int, event: ParkingSensorDto): CoachingSocketDto {
		return createCoachingEvent(step, event)
    }

	private fun createCoachingEvent(step: Int, event: ParkingSensorDto): CoachingSocketDto {
		val targetAngleDeg = ParkingReference.coachingTargetAngleDeg(step)
		val targetDistanceCm = ParkingReference.coachingTargetMoveDistanceCm(step)
		val stepStart = ParkingReference.coachingStepStart(step)
		val currentMoveDistanceCmRaw = if (stepStart == null) {
			0
		} else if (step == 1) {
			// step1 is straight: progress only on x-axis
			((event.x - stepStart.x) * 100).roundToInt()
		} else {
			(kotlin.math.hypot(event.x - stepStart.x, event.y - stepStart.y) * 100).roundToInt()
		}
		val currentMoveDistanceCm = currentMoveDistanceCmRaw
			.coerceAtLeast(0)
			.let { if (targetDistanceCm > 0) it.coerceAtMost(targetDistanceCm) else it }
		val distancesCm = ObstacleDistancesDto(
			frontDistance = (event.frontDist * 100).roundToInt(),
			backDistance = (event.rearDist * 100).roundToInt(),
			leftDistance = (event.leftDist * 100).roundToInt(),
			rightDistance = (event.rightDist * 100).roundToInt(),
		)

		val coachingId = when {
			distancesCm.backDistance <= DANGER_LIMIT_FRONT_BACK -> 1
			distancesCm.frontDistance <= DANGER_LIMIT_FRONT_BACK -> 2
			distancesCm.leftDistance <= DANGER_LIMIT_SIDE -> 3
			distancesCm.rightDistance <= DANGER_LIMIT_SIDE -> 4
			else -> 5
		}

		return CoachingSocketDto(
            step = step,
            timestamp = System.currentTimeMillis(),
			targetAngle = targetAngleDeg,
			targetDistance = targetDistanceCm,
			currentAngle = event.handleAngle.roundToInt(),
			currentDistance = currentMoveDistanceCm,
			distances = distancesCm,
            coachingId = coachingId,
        )
	}
}
