package com.parkit.analysis.driving.persistence.document

import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.index.CompoundIndex
import org.springframework.data.mongodb.core.index.CompoundIndexes
import org.springframework.data.mongodb.core.mapping.Document
import java.time.Instant

@Document("sensor_logs")
@CompoundIndexes(
	CompoundIndex(name = "session_time_idx", def = "{'sessionId': 1, 'time': 1}"),
)
data class SensorLogDocument(
	@Id
	val id: String? = null,
	val sessionId: String,
	val receivedAt: Instant,
	val time: Double,
	val x: Double,
	val y: Double,
	val z: Double,
	val steer: Double,
	val wheelDegree: Double,
	val handleAngle: Double,
	val speed: Double,
	val frontDist: Double,
	val leftDist: Double,
	val rightDist: Double,
	val rearDist: Double,
)
