package com.parkit.socket.controller

import com.parkit.socket.dto.CoachingData
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
        private const val TOPIC_DESTINATION = "/topic/coaching"
        private val WARNING_LEVELS = listOf("SAFE", "WARNING", "DANGER")
    }

    // 1초마다 가짜 주차 코칭 데이터를 브로드캐스트
    @Scheduled(fixedRate = 1000)
    fun sendMockCoachingData() {
        val mockData = CoachingData(
            sensorId = "front_left",
            distance = Math.random() * 100,
            warningLevel = WARNING_LEVELS.random(),
            message = "주차 각도를 5도 조정하세요.",
            timestamp = System.currentTimeMillis()
        )

        // 브로커의 /topic/coaching 을 구독하는 클라이언트들에게 전달
        messagingTemplate.convertAndSend(TOPIC_DESTINATION, mockData)
    }
}
