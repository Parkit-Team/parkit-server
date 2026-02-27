package com.parkit.socket.controller

import org.springframework.messaging.simp.SimpMessagingTemplate
import org.springframework.scheduling.annotation.EnableScheduling
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Controller

@Controller
@EnableScheduling
class MockCoachingController(
    private val messagingTemplate: SimpMessagingTemplate
) {

    // 1초마다 가짜 주차 코칭 데이터를 브로드캐스트
    @Scheduled(fixedRate = 1000)
    fun sendMockCoachingData() {
        val mockData = mapOf(
            "sensor_id" to "front_left",
            "distance" to Math.random() * 100,
            "warning_level" to listOf("SAFE", "WARNING", "DANGER").random(),
            "message" to "주차 각도를 5도 조정하세요.",
            "timestamp" to System.currentTimeMillis()
        )

        // 브로커의 /topic/coaching 을 구독하는 클라이언트들에게 전달
        messagingTemplate.convertAndSend("/topic/coaching", mockData as Any)
    }
}
