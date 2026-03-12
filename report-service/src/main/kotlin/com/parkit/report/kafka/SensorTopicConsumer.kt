package com.parkit.report.kafka

import com.fasterxml.jackson.databind.ObjectMapper
import com.parkit.report.driving.service.SensorLogService
import com.parkit.report.driving.service.DrivingSessionService
import com.parkit.report.kafka.dto.ParkingSensorDto
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.slf4j.LoggerFactory
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.stereotype.Component

@Component
@ConditionalOnProperty(prefix = "parkit.kafka", name = ["enabled"], havingValue = "true", matchIfMissing = true)
class SensorTopicConsumer(
	private val drivingSessionService: DrivingSessionService,
	private val sensorLogService: SensorLogService,
	private val objectMapper: ObjectMapper,
) {
	private val log = LoggerFactory.getLogger(javaClass)

	@KafkaListener(topics = ["\${parkit.kafka.topics.sensor}"])
	fun consume(record: ConsumerRecord<String, String>) {
		val message = record.value()
		try {
			val runningSession = record.key()?.let { drivingSessionService.findRunningById(it) }
				?: drivingSessionService.findLatestRunning()
			if (runningSession == null) {
				log.debug("Ignored sensor-topic message: no RUNNING session")
				return
			}

			drivingSessionService.attachFirstSensorPayloadIfAbsent(runningSession.id, message)
			val dto = objectMapper.readValue(message, ParkingSensorDto::class.java)
			sensorLogService.append(runningSession.id, dto)
		} catch (e: Exception) {
			log.error("Failed to consume sensor-topic message", e)
		}
	}
}
