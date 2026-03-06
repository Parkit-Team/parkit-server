package com.parkit.analysis.publisher

import com.parkit.analysis.dto.CoachingAlertEvent
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

/**
 * Kafka Producer 로직 연결되기 전까지 빌드/실행을 위한 임시 Bean 구현체
 */
@Component
class DummyCoachingEventPublisher : CoachingEventPublisher {

    private val log = LoggerFactory.getLogger(DummyCoachingEventPublisher::class.java)

    override fun publish(event: CoachingAlertEvent) {
        log.info("[Dummy Publisher] 알림 이벤트 발행됨 - 센서ID: {}, 위험수준: {}, 메시지: {}", 
            event.sensorId, event.level, event.message)
    }
}
