package com.parkit.analysis.kafka

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.parkit.analysis.coaching.service.RiskDetectionService
import com.parkit.analysis.kafka.dto.ParkingSensorDto
import com.parkit.analysis.parking.domain.ParkingEvent
import com.parkit.analysis.parking.service.ParkingScoringService
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.stereotype.Component

@Component
class KafkaAnalysisConsumer(
	private val riskDetectionService: RiskDetectionService,
	private val parkingScoringService: ParkingScoringService,
	private val kafkaTemplate: KafkaTemplate<String, String>,
	private val objectMapper: ObjectMapper,
	@Value("\${parkit.kafka.topics.coachingEvent}")
	private val coachingEventTopic: String,
) {
	private val log = LoggerFactory.getLogger(KafkaAnalysisConsumer::class.java)

	/**
	 * 타겟 토픽(sensor-topic)을 구독
	 */
	@KafkaListener(topics = ["\${parkit.kafka.topics.sensor}"])
	fun consume(record: ConsumerRecord<String, String>) {
		val sessionId = record.key() ?: "unknown-session"
		val message = record.value()
		val analysisReceivedAtEpochMs = System.currentTimeMillis()
		
		try {
			log.debug("Received Kafka message for session {}: {}", sessionId, message)
			val sensorDto = objectMapper.readValue(message, ParkingSensorDto::class.java)
			val parkingEvent = ParkingEvent(
				time = sensorDto.time,
				x = sensorDto.x,
				y = sensorDto.y,
				z = sensorDto.z,
				handleAngle = sensorDto.handleAngle,
				sensor = ParkingEvent.SensorData(
					frontDistance = sensorDto.frontDist,
					leftDistance = sensorDto.leftDist,
					rightDistance = sensorDto.rightDist,
					rearDistance = sensorDto.rearDist,
					speed = sensorDto.speed,
				),
			)

			// 세션별 순차 처리를 위해 동기 방식으로 처리
			val result = parkingScoringService.processParkingEvent(sessionId, parkingEvent).block()
			
			if (result != null) {
				val coaching = riskDetectionService.calculate(
					result.step,
					sensorDto,
					result.initialX,
					result.initialY,
					analysisReceivedAtEpochMs,
				)
				val coachingJson = objectMapper.writeValueAsString(coaching)
				
				// Kafka 전송도 완료될 때까지 대기
				kafkaTemplate.send(coachingEventTopic, sessionId, coachingJson).get()
			}
				
		} catch (e: Exception) {
			log.error("Failed to process Kafka message for session {}", sessionId, e)
		}
	}
}
