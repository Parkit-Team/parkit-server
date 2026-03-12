package com.parkit.report.driving.service

import com.parkit.report.driving.persistence.document.SensorLogDocument
import com.parkit.report.driving.persistence.repository.SensorLogMongoRepository
import com.parkit.report.kafka.dto.ParkingSensorDto
import org.springframework.stereotype.Service
import java.time.Clock
import java.time.Instant

@Service
class SensorLogService(
	private val sensorLogRepository: SensorLogMongoRepository,
	private val clock: Clock = Clock.systemUTC(),
) {
	fun append(sessionId: String, event: ParkingSensorDto): SensorLogDocument {
		val now = Instant.now(clock)
		val doc = SensorLogDocument(
			sessionId = sessionId,
			receivedAt = now,
			time = event.time,
			x = event.x,
			y = event.y,
			z = event.z,
			steer = event.steer,
			wheelDegree = event.wheelDegree,
			handleAngle = event.handleAngle,
			speed = event.speed,
			frontDist = event.frontDist,
			leftDist = event.leftDist,
			rightDist = event.rightDist,
			rearDist = event.rearDist,
		)
		return sensorLogRepository.save(doc)
	}

	fun getSensorLogs(sessionId: String, limit: Long): List<SensorLogDocument> =
		sensorLogRepository.findBySessionIdOrderByTimeAsc(sessionId).take(limit.toInt())
}
