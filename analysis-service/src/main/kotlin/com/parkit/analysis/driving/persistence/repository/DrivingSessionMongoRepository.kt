package com.parkit.analysis.driving.persistence.repository

import com.parkit.analysis.driving.domain.DrivingSessionStatus
import com.parkit.analysis.driving.persistence.document.DrivingSessionDocument

import org.springframework.data.mongodb.repository.ReactiveMongoRepository
import reactor.core.publisher.Mono

interface DrivingSessionMongoRepository : ReactiveMongoRepository<DrivingSessionDocument, String> {
	fun findFirstByStatusOrderByStartedAtDesc(status: DrivingSessionStatus): Mono<DrivingSessionDocument>
}
