package com.parkit.analysis.coaching.service

import com.parkit.analysis.coaching.dto.CoachingSocketDto
import com.parkit.analysis.coaching.dto.ObstacleDistancesDto
import com.parkit.analysis.kafka.dto.ParkingSensorDto
import com.parkit.analysis.parking.domain.ParkingReference
import org.springframework.stereotype.Service
import kotlin.math.roundToInt
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.sqrt

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
		val minDistance = minDistance(event)
		return createCoachingEvent(step, event, minDistance)
    }

    /**
     * 거리 데이터를 기반으로 코칭 이벤트 반환
     */
	fun createCoachingEvent(step: Int, event: ParkingSensorDto): CoachingSocketDto {
		val minDistance = minDistance(event)
		return createCoachingEvent(step, event, minDistance)
	}

        private fun createCoachingEvent(step: Int, event: ParkingSensorDto, minDistance: Double): CoachingSocketDto {
		val ref = ParkingReference.getReferenceForStep(step)
		val targetAngle = ref?.handleAngle ?: 0.0
		val targetDistance = if (ref == null) 0.0 else {
            sqrt((event.x - ref.x).pow(2) + (event.y - ref.y).pow(2))
		}
		val frontGap = (event.frontDist * 100).roundToInt()
		val backGap = (event.rearDist * 100).roundToInt()
		val leftGap = (event.leftDist * 100).roundToInt()
		val rightGap = (event.rightDist * 100).roundToInt()

		val coachingId = when {
			backGap <= DANGER_LIMIT_FRONT_BACK -> 1
			frontGap <= DANGER_LIMIT_FRONT_BACK -> 2
			leftGap <= DANGER_LIMIT_SIDE -> 3
			rightGap <= DANGER_LIMIT_SIDE -> 4
			else -> 5
		}

		return CoachingSocketDto(
            step = step,
            timestamp = System.currentTimeMillis(),
            targetAngle = targetAngle,
            targetDistance = targetDistance,
            currentAngle = event.handleAngle,
            currentDistance = minDistance,
			distances = ObstacleDistancesDto(
				frontDistance = frontGap,
				backDistance = backGap,
				leftDistance = leftGap,
				rightDistance = rightGap,
			),
            coachingId = coachingId,
        )
	}

	private fun minDistance(event: ParkingSensorDto): Double {
		return min(
            min(event.frontDist, event.rearDist),
            min(event.leftDist, event.rightDist),
        )
	}
}
