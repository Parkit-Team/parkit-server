package com.parkit.analysis.kafka

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.parkit.analysis.coaching.service.RiskDetectionService
import com.parkit.analysis.kafka.dto.ParkingSensorDto
import com.parkit.analysis.kafka.mapper.toParkingEvent
import com.parkit.analysis.parking.service.ParkingScoringService
import org.slf4j.LoggerFactory
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.stereotype.Component
import reactor.core.scheduler.Schedulers

@Component
class KafkaAnalysisConsumer(
    private val riskDetectionService: RiskDetectionService,
    private val parkingScoringService: ParkingScoringService,
    private val kafkaTemplate: KafkaTemplate<String, String>,
    private val objectMapper: ObjectMapper
) {
    private val log = LoggerFactory.getLogger(KafkaAnalysisConsumer::class.java)

    /**
     * 타겟 토픽(sensor-topic)을 구독
     */
    @KafkaListener(topics = ["sensor-topic"], groupId = "analysis-group")
    fun consume(message: String) {
        try {
            // 수신 된 JSON 페이로드를 ParkingSensorEvent 객체로 역직렬화
            val event = objectMapper.readValue<ParkingSensorDto>(message)

//            println("✅ 수신된 주차 데이터: ${event.status} | 메시지: ${event.msg}")
            log.debug("Received Kafka message: {}", message)

			// 해당 이벤트를 RiskDetectionService로 전달하여 처리
			// 주차 채점 평가 (세션 아이디는 현재 없으므로 'default-session'으로 통일)
			val sessionId = "default-session"
			parkingScoringService.processParkingEvent(sessionId, event.toParkingEvent())
				.publishOn(Schedulers.boundedElastic())
				.subscribe(
					{ result ->
						riskDetectionService.calculate(result.step, event)
							?.let { coaching ->
								val coachingJson = objectMapper.writeValueAsString(coaching)
								kafkaTemplate.send("coaching-event", sessionId, coachingJson)
							}
						val resultJson = objectMapper.writeValueAsString(result)
						kafkaTemplate.send("parking-score-result", sessionId, resultJson)
					},
					{ error -> log.error("Scoring failed for session: {}", sessionId, error) }
				)

        } catch (e: Exception) {
            log.error("Failed to process Kafka message from 'sensor-topic' topic", e)
        }
    }
}
