package com.parkit.analysis.parking.repository

import com.parkit.analysis.parking.domain.ParkingStepState
import reactor.core.publisher.Mono

interface ParkingStepStateRepository {
	fun save(state: ParkingStepState): Mono<Boolean>

	fun findById(sessionId: String): Mono<ParkingStepState>

	fun deleteById(sessionId: String): Mono<Boolean>
}