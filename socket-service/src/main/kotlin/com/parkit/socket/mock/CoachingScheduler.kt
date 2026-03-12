package com.parkit.socket.mock

import com.parkit.socket.dto.CoachingSocketDto
import com.parkit.socket.dto.ObstacleDistancesDto
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.messaging.simp.SimpMessagingTemplate
import org.springframework.scheduling.annotation.EnableScheduling
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import kotlin.random.Random

@Component
@EnableScheduling
@ConditionalOnProperty(prefix = "parkit.mock.coaching", name = ["enabled"], havingValue = "true")
class CoachingScheduler(
	private val messagingTemplate: SimpMessagingTemplate,
) {
	companion object {
		private const val TOPIC_DESTINATION = "/topic/coaching-mock"
		private const val DANGER_LIMIT_FRONT_BACK = 200
		private const val DANGER_LIMIT_SIDE = 80
	}

	@Scheduled(fixedRate = 1000)
	fun sendMockCoachingData() {
		val frontGap = Random.nextInt(50, 301)
		val backGap = Random.nextInt(50, 301)
		val leftGap = Random.nextInt(30, 151)
		val rightGap = Random.nextInt(30, 151)

		val coachingId = when {
			backGap <= DANGER_LIMIT_FRONT_BACK -> 1
			frontGap <= DANGER_LIMIT_FRONT_BACK -> 2
			leftGap <= DANGER_LIMIT_SIDE -> 3
			rightGap <= DANGER_LIMIT_SIDE -> 4
			else -> 5
		}

		val mockData = CoachingSocketDto(
			step = 1,
			timestamp = System.currentTimeMillis(),
            targetAngle = 0,
            targetDistance = 100,
            currentAngle = 0,
            currentDistance = Random.nextInt(0, 201),
			distances = ObstacleDistancesDto(
				frontDistance = frontGap,
				backDistance = backGap,
				leftDistance = leftGap,
				rightDistance = rightGap,
			),
			coachingId = coachingId,
		)

		messagingTemplate.convertAndSend(TOPIC_DESTINATION, mockData)
	}
}
