package com.parkit.analysis.kafka

import com.fasterxml.jackson.databind.ObjectMapper
import com.parkit.analysis.dto.VideoAnalysisEvent
import com.parkit.analysis.service.RiskDetectionService
import org.slf4j.LoggerFactory
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.stereotype.Component

@Component
class KafkaAnalysisConsumer(
    private val riskDetectionService: RiskDetectionService,
    private val objectMapper: ObjectMapper
) {
    private val log = LoggerFactory.getLogger(KafkaAnalysisConsumer::class.java)

    /**
     * 타겟 토픽(analysis-events)을 구독
     */
    @KafkaListener(topics = ["analysis-events"], groupId = "analysis-service-group")
    fun consume(message: String) {
        try {
            log.debug("Received Kafka message: {}", message)
            
            // 수신 된 JSON 페이로드를 VideoAnalysisEvent 객체로 역직렬화
            val event = objectMapper.readValue(message, VideoAnalysisEvent::class.java)
            
            // 해당 이벤트를 RiskDetectionService로 전달하여 처리
            riskDetectionService.processEvent(event)
            
        } catch (e: Exception) {
            log.error("Failed to process Kafka message from 'analysis-events' topic", e)
        }
    }
}
