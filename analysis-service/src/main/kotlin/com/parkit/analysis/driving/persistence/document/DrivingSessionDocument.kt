package com.parkit.analysis.driving.persistence.document

import com.parkit.analysis.driving.domain.DrivingSessionStatus

import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.mapping.Document
import java.time.Instant

@Document("driving_sessions")
data class DrivingSessionDocument(
	@Id
	val id: String,
	val userId: String?,
	val status: DrivingSessionStatus,
	val startedAt: Instant,
	val stoppedAt: Instant?,
	val frontendScore: Double?,
)
