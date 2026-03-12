package com.parkit.report.driving.service

import com.parkit.report.driving.domain.DrivingSessionStatus
import com.parkit.report.driving.persistence.document.DrivingSessionDocument
import com.parkit.report.driving.persistence.repository.DrivingSessionMongoRepository

import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.web.server.ResponseStatusException
import java.time.Clock
import java.time.Instant
import java.util.UUID

@Service
class DrivingSessionService(
	private val drivingSessionRepository: DrivingSessionMongoRepository,
	private val clock: Clock = Clock.systemUTC(),
) {
	fun start(userId: String?): DrivingSessionDocument {
		val existing = findLatestRunning()
		if (existing != null) {
			return existing
		}
		
		val now = Instant.now(clock)
		val session = DrivingSessionDocument(
			id = UUID.randomUUID().toString(),
			userId = userId,
			status = DrivingSessionStatus.RUNNING,
			startedAt = now,
			stoppedAt = null,
			frontendScore = null,
		)
		return drivingSessionRepository.save(session)
	}

	fun stop(sessionId: String, frontendScore: Double): DrivingSessionDocument {
		val now = Instant.now(clock)
		val existing = drivingSessionRepository.findById(sessionId)
			.orElseThrow { ResponseStatusException(HttpStatus.NOT_FOUND, "Session not found") }
		
		val updated = existing.copy(
			status = DrivingSessionStatus.STOPPED,
			stoppedAt = now,
			frontendScore = frontendScore,
		)
		return drivingSessionRepository.save(updated)
	}

	fun findLatestRunning(): DrivingSessionDocument? =
		drivingSessionRepository.findFirstByStatusOrderByStartedAtDesc(DrivingSessionStatus.RUNNING).orElse(null)

	fun get(sessionId: String): DrivingSessionDocument =
		drivingSessionRepository.findById(sessionId)
			.orElseThrow { ResponseStatusException(HttpStatus.NOT_FOUND, "Session not found") }
}
