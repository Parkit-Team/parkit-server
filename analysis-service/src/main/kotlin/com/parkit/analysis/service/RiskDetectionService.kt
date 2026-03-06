package com.parkit.analysis.service

import com.parkit.analysis.domain.CoachingLevel
import com.parkit.analysis.dto.CoachingAlertEvent
import com.parkit.analysis.dto.ParkingSensorEvent
import com.parkit.analysis.publisher.CoachingEventPublisher
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
class RiskDetectionService(
    private val coachingEventPublisher: CoachingEventPublisher
) {
    private val log = LoggerFactory.getLogger(RiskDetectionService::class.java)

    /**
     * 주차 센서와 장애물 간의 거리 - 임시값
     */
    companion object {
        const val DANGER_THRESHOLD = 0.3    // 30cm 미만
        const val WARNING_THRESHOLD = 1.0   // 1m 이하
    }

    /**
     * Kafka에서 수신한 ParkingSensorEvent를 통해 실시간 위험을 판단하고 알림을 발행
     */
    fun processEvent(event: ParkingSensorEvent) {
        log.info("Processing ParkingSensorEvent - status: {}, msg: {}", event.status, event.msg)
        
        val alertEvent = detectRisk(event)
        
        if (alertEvent != null) {
            log.info("Risk detected: {} - {}", alertEvent.level, alertEvent.message)
            coachingEventPublisher.publish(alertEvent)
        }
    }

    /**
     * 거리 데이터를 기반으로 코칭 이벤트 반환
     */
    fun detectRisk(event: ParkingSensorEvent): CoachingAlertEvent? {
        // 임시로 msg 필드의 값을 Double(거리)로 파싱 시도
        val parsedDistance = event.msg.toDoubleOrNull() ?: return null
        val tempSensorId = 1 // 임시 센서 ID 부여

        return when {
            parsedDistance < DANGER_THRESHOLD -> CoachingAlertEvent(
                sensorId = tempSensorId,
                level = CoachingLevel.DANGER,
                message = "충돌 위험! 정지하세요",
                timestamp = System.currentTimeMillis()
            )
            parsedDistance <= WARNING_THRESHOLD -> CoachingAlertEvent(
                sensorId = tempSensorId,
                level = CoachingLevel.WARNING,
                message = "장애물이 가까워집니다. 속도를 줄이세요",
                timestamp = System.currentTimeMillis()
            )
            else -> CoachingAlertEvent(
                sensorId = tempSensorId,
                level = CoachingLevel.SAFE,
                message = "그대로 후진하세요",
                timestamp = System.currentTimeMillis()
            )
        }
    }
}
