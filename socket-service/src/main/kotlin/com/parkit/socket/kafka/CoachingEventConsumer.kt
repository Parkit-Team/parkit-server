package com.parkit.socket.kafka

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.parkit.socket.dto.CoachingSocketDto
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.messaging.simp.SimpMessagingTemplate
import org.springframework.stereotype.Component

@Component
class CoachingEventConsumer(
	private val messagingTemplate: SimpMessagingTemplate,
	private val objectMapper: ObjectMapper,
    @Value("\${parkit.kafka.topics.coachingEvent}")
    private val coachingEventTopic: String,
) {
	private val log = LoggerFactory.getLogger(javaClass)

	companion object {
		private const val TOPIC_DESTINATION = "/topic/coaching"
	}

    @KafkaListener(topics = ["\${parkit.kafka.topics.coachingEvent}"])
	fun consume(message: String) {
		try {
			val event = objectMapper.readValue<CoachingSocketDto>(message)
			messagingTemplate.convertAndSend(
				TOPIC_DESTINATION,
				event.copy(socketForwardedAtEpochMs = System.currentTimeMillis()),
			)
		} catch (e: Exception) {
			log.error("Failed to process coaching-event message", e)
		}
	}
}
