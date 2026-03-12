package com.parkit.report.driving.persistence.repository

import com.parkit.report.driving.domain.DrivingSessionStatus
import com.parkit.report.driving.persistence.document.DrivingSessionDocument

import org.springframework.data.mongodb.repository.MongoRepository
import java.util.Optional

interface DrivingSessionMongoRepository : MongoRepository<DrivingSessionDocument, String> {
	fun findFirstByStatusOrderByStartedAtDesc(status: DrivingSessionStatus): Optional<DrivingSessionDocument>
}
