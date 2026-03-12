package com.parkit.report.driving.persistence.repository

import com.parkit.report.driving.persistence.document.SensorLogDocument
import org.springframework.data.mongodb.repository.MongoRepository

interface SensorLogMongoRepository : MongoRepository<SensorLogDocument, String> {
	fun findBySessionIdOrderByTimeAsc(sessionId: String): List<SensorLogDocument>
}
