package com.parkit.analysis.driving.service

import com.parkit.analysis.driving.domain.DrivingSessionStatus
import com.parkit.analysis.driving.persistence.document.DrivingSessionDocument
import com.parkit.analysis.driving.persistence.repository.DrivingSessionMongoRepository

import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.web.server.ResponseStatusException
import reactor.core.publisher.Mono
import java.time.Clock
import java.time.Instant
import java.util.UUID

@Service
class DrivingSessionService(
	private val drivingSessionRepository: DrivingSessionMongoRepository,
	private val clock: Clock = Clock.systemUTC(),
) {
	fun start(userId: String?): Mono<DrivingSessionDocument> {
		return findLatestRunning()
			.switchIfEmpty(
				Mono.defer {
					val now = Instant.now(clock)
					val session = DrivingSessionDocument(
						id = UUID.randomUUID().toString(),
						userId = userId,
						status = DrivingSessionStatus.RUNNING,
						startedAt = now,
						stoppedAt = null,
						frontendScore = null,
					)
					drivingSessionRepository.save(session)
				}
			)
	}

	fun stop(sessionId: String, frontendScore: Double): Mono<DrivingSessionDocument> {
		val now = Instant.now(clock)
		return drivingSessionRepository.findById(sessionId)
			.switchIfEmpty(Mono.error(ResponseStatusException(HttpStatus.NOT_FOUND, "Session not found")))
			.flatMap { existing ->
				val updated = existing.copy(
					status = DrivingSessionStatus.STOPPED,
					stoppedAt = now,
					frontendScore = frontendScore,
				)
				drivingSessionRepository.save(updated)
			}
	}

	fun findLatestRunning(): Mono<DrivingSessionDocument> =
		drivingSessionRepository.findFirstByStatusOrderByStartedAtDesc(DrivingSessionStatus.RUNNING)

	fun get(sessionId: String): Mono<DrivingSessionDocument> =
		drivingSessionRepository.findById(sessionId)
			.switchIfEmpty(Mono.error(ResponseStatusException(HttpStatus.NOT_FOUND, "Session not found")))
}
