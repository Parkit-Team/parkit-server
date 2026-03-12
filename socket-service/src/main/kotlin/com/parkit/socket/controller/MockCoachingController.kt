package com.parkit.socket.controller

import com.parkit.socket.dto.CoachingSocketDto
import com.parkit.socket.dto.ObstacleDistancesDto
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.messaging.simp.SimpMessagingTemplate
import org.springframework.scheduling.annotation.EnableScheduling
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Controller
import kotlin.random.Random

@Controller
@EnableScheduling
@ConditionalOnProperty(prefix = "parkit.mock.coaching", name = ["enabled"], havingValue = "true")
class MockCoachingController(
    private val messagingTemplate: SimpMessagingTemplate
) {

	companion object {
		private const val TOPIC_DESTINATION = "/topic/coaching-mock"
		private const val DANGER_LIMIT_FRONT_BACK = 200
		private const val DANGER_LIMIT_SIDE = 80
	}

    // 1초마다 가짜 주차 코칭 데이터를 브로드캐스트
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
			targetAngle = 0.0,
			targetDistance = 1.0,
			currentAngle = 0.0,
			currentDistance = Random.nextDouble(0.0, 2.0),
			distances = ObstacleDistancesDto(
				frontDistance = frontGap,
				backDistance = backGap,
				leftDistance = leftGap,
				rightDistance = rightGap,
			),
			coachingId = coachingId,
		)

        // 브로커의 /topic/coaching 을 구독하는 클라이언트들에게 전달
		messagingTemplate.convertAndSend(TOPIC_DESTINATION, mockData)
    }
}
