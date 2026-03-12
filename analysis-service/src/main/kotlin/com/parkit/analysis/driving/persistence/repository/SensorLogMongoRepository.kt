package com.parkit.analysis.driving.persistence.repository

import com.parkit.analysis.driving.persistence.document.SensorLogDocument
import org.springframework.data.mongodb.repository.ReactiveMongoRepository
import reactor.core.publisher.Flux

interface SensorLogMongoRepository : ReactiveMongoRepository<SensorLogDocument, String> {
	fun findBySessionIdOrderByTimeAsc(sessionId: String): Flux<SensorLogDocument>
}
