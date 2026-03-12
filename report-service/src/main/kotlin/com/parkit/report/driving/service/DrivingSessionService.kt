package com.parkit.report.driving.service

import com.parkit.report.driving.domain.DrivingSessionStatus
import com.parkit.report.driving.persistence.document.DrivingSessionDocument
import com.parkit.report.driving.persistence.document.SensorLogDocument
import com.parkit.report.driving.persistence.repository.DrivingSessionMongoRepository

import org.springframework.data.mongodb.core.FindAndModifyOptions
import org.springframework.data.mongodb.core.MongoTemplate
import org.springframework.data.mongodb.core.query.Criteria
import org.springframework.data.mongodb.core.query.Query
import org.springframework.data.mongodb.core.query.Update
import org.springframework.stereotype.Service
import org.springframework.web.server.ResponseStatusException
import java.time.Clock
import java.time.Instant
import java.util.UUID
import org.springframework.http.HttpStatus

@Service
class DrivingSessionService(
	private val drivingSessionRepository: DrivingSessionMongoRepository,
	private val mongoTemplate: MongoTemplate,
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
			firstSensorPayloadJson = null,
			firstSensorReceivedAt = null,
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
		val savedSession = drivingSessionRepository.save(updated)

		// stop 시점에 해당 세션의 모든 센서 로그에 점수를 일괄 반영
		val scoreQuery = Query(Criteria.where("sessionId").`is`(sessionId))
		val scoreUpdate = Update().set("frontendScore", frontendScore)
		mongoTemplate.updateMulti(scoreQuery, scoreUpdate, SensorLogDocument::class.java)

		return savedSession
	}

	fun findLatestRunning(): DrivingSessionDocument? =
		drivingSessionRepository.findFirstByStatusOrderByStartedAtDesc(DrivingSessionStatus.RUNNING).orElse(null)

	fun findRunningById(sessionId: String): DrivingSessionDocument? =
		drivingSessionRepository.findById(sessionId).orElse(null)?.takeIf { it.status == DrivingSessionStatus.RUNNING }

	fun get(sessionId: String): DrivingSessionDocument =
		drivingSessionRepository.findById(sessionId)
			.orElseThrow { ResponseStatusException(HttpStatus.NOT_FOUND, "Session not found") }

	fun attachFirstSensorPayloadIfAbsent(sessionId: String, messageJson: String): DrivingSessionDocument? {
		val now = Instant.now(clock)
		val query = Query()
			.addCriteria(Criteria.where("id").`is`(sessionId))
			.addCriteria(Criteria.where("status").`is`(DrivingSessionStatus.RUNNING))
			.addCriteria(
				Criteria().orOperator(
					Criteria.where("firstSensorPayloadJson").exists(false),
					Criteria.where("firstSensorPayloadJson").`is`(null),
				),
			)

		val update = Update()
			.set("firstSensorPayloadJson", messageJson)
			.set("firstSensorReceivedAt", now)

		return mongoTemplate.findAndModify(
			query,
			update,
			FindAndModifyOptions.options().returnNew(true),
			DrivingSessionDocument::class.java,
		)
	}
}
