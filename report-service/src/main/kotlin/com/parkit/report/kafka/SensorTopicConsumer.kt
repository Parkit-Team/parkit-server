package com.parkit.report.kafka

import com.parkit.report.driving.service.DrivingSessionService
import org.slf4j.LoggerFactory
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.stereotype.Component

@Component
@ConditionalOnProperty(prefix = "parkit.kafka", name = ["enabled"], havingValue = "true", matchIfMissing = true)
class SensorTopicConsumer(
	private val drivingSessionService: DrivingSessionService,
) {
	private val log = LoggerFactory.getLogger(javaClass)

	@KafkaListener(topics = ["\${parkit.kafka.topics.sensor}"])
	fun consume(message: String) {
		try {
			val updated = drivingSessionService.attachFirstSensorPayloadIfAbsent(message)
			if (updated != null) {
				log.info("Attached first sensor payload to sessionId={}", updated.id)
			}
		} catch (e: Exception) {
			log.error("Failed to consume sensor-topic message", e)
		}
	}
}
