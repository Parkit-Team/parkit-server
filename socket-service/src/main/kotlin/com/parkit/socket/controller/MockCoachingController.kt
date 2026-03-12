package com.parkit.socket.controller

import com.parkit.socket.dto.CoachingSocketDto
import com.parkit.socket.dto.ObstacleDistancesDto
import org.springframework.messaging.simp.SimpMessagingTemplate
import org.springframework.scheduling.annotation.EnableScheduling
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Controller

@Controller
@EnableScheduling
class MockCoachingController(
    private val messagingTemplate: SimpMessagingTemplate
) {

	companion object {
		private const val TOPIC_DESTINATION = "/topic/coaching-mock"
	}

    // 1초마다 가짜 주차 코칭 데이터를 브로드캐스트
    @Scheduled(fixedRate = 1000)
    fun sendMockCoachingData() {
		val mockData = CoachingSocketDto(
			step = 1,
			timestamp = System.currentTimeMillis(),
			targetAngle = 0.0,
			targetDistance = 1.0,
			currentAngle = 0.0,
			currentDistance = Math.random() * 2,
			distances = ObstacleDistancesDto(
				frontDistance = Math.random() * 2,
				backDistance = Math.random() * 2,
				leftDistance = Math.random() * 2,
				rightDistance = Math.random() * 2,
			),
			coachingId = 1,
		)

        // 브로커의 /topic/coaching 을 구독하는 클라이언트들에게 전달
		messagingTemplate.convertAndSend(TOPIC_DESTINATION, mockData)
    }
}
