package com.parkit.analysis.kafka

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.parkit.analysis.coaching.service.RiskDetectionService
import com.parkit.analysis.kafka.dto.ParkingSensorDto
import com.parkit.analysis.kafka.mapper.toParkingEvent
import com.parkit.analysis.parking.service.ParkingScoringService
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.stereotype.Component
import reactor.core.publisher.Mono
import reactor.core.scheduler.Schedulers

@Component
class KafkaAnalysisConsumer(
    private val riskDetectionService: RiskDetectionService,
    private val parkingScoringService: ParkingScoringService,
    private val kafkaTemplate: KafkaTemplate<String, String>,
    private val objectMapper: ObjectMapper,
    @Value("\${parkit.kafka.topics.coachingEvent}")
    private val coachingEventTopic: String,
    @Value("\${parkit.kafka.topics.parkingScoreResult}")
    private val parkingScoreResultTopic: String,
) {
    private val log = LoggerFactory.getLogger(KafkaAnalysisConsumer::class.java)

    /**
     * 타겟 토픽(sensor-topic)을 구독
     */
    @KafkaListener(topics = ["\${parkit.kafka.topics.sensor}"])
    fun consume(message: String) {
        try {
            // 수신 된 JSON 페이로드를 ParkingSensorEvent 객체로 역직렬화
            val event = objectMapper.readValue<ParkingSensorDto>(message)

//            println("✅ 수신된 주차 데이터: ${event.status} | 메시지: ${event.msg}")
            log.debug("Received Kafka message: {}", message)

            // UUID 임시 발급 또는 세션 연결 우회 로직 필요
            // session id를 Kafka 메시지에서 가져오거나 별도 처리
            val sessionId = "unknown-session"

            parkingScoringService.processParkingEvent(sessionId, event.toParkingEvent())
                .publishOn(Schedulers.boundedElastic())
                .doOnNext { result ->
							val coaching = riskDetectionService.calculate(result.step, event, result.initialX, result.initialY)
							val coachingJson = objectMapper.writeValueAsString(coaching)
							kafkaTemplate.send(coachingEventTopic, sessionId, coachingJson)

                    val resultJson = objectMapper.writeValueAsString(result)
                    kafkaTemplate.send(parkingScoreResultTopic, sessionId, resultJson)
                }
                .then()
                .subscribe(
                    { /* no-op */ },
                    { error -> log.error("Failed to process sensor event", error) },
                )

        } catch (e: Exception) {
            log.error("Failed to process Kafka message from 'sensor-topic' topic", e)
        }
    }
}
