package com.parkit.analysis.driving.api

import com.parkit.analysis.driving.api.dto.DrivingSensorLogRequest
import com.parkit.analysis.driving.api.dto.StartDrivingSessionRequest
import com.parkit.analysis.driving.api.dto.StartDrivingSessionResponse
import com.parkit.analysis.driving.api.dto.StopDrivingSessionRequest
import com.parkit.analysis.driving.service.DrivingSessionService
import com.parkit.analysis.driving.persistence.document.DrivingSessionDocument
import com.parkit.analysis.driving.persistence.document.SensorLogDocument
import com.parkit.analysis.driving.persistence.repository.SensorLogMongoRepository
import com.parkit.analysis.driving.service.SensorLogService
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

@RestController
@RequestMapping("/api/driving-sessions")
class DrivingSessionController(
	private val drivingSessionService: DrivingSessionService,
	private val sensorLogService: SensorLogService,
	private val sensorLogRepository: SensorLogMongoRepository,
) {
	@PostMapping("/start")
	@ResponseStatus(HttpStatus.CREATED)
	fun start(@RequestBody(required = false) request: StartDrivingSessionRequest?): Mono<StartDrivingSessionResponse> =
		drivingSessionService.start(request?.userId)
			.map { StartDrivingSessionResponse(sessionId = it.id, startedAt = it.startedAt) }

	@PostMapping("/{sessionId}/stop")
	fun stop(
		@PathVariable sessionId: String,
		@RequestBody request: StopDrivingSessionRequest,
	): Mono<DrivingSessionDocument> = drivingSessionService.stop(sessionId, request.frontendScore)

	@PostMapping("/{sessionId}/sensor-logs")
	@ResponseStatus(HttpStatus.CREATED)
	fun appendSensorLog(
		@PathVariable sessionId: String,
		@RequestBody request: DrivingSensorLogRequest,
	): Mono<SensorLogDocument> = sensorLogService.append(sessionId, request.toDto())

	@GetMapping("/{sessionId}")
	fun get(@PathVariable sessionId: String): Mono<DrivingSessionDocument> = drivingSessionService.get(sessionId)

	@GetMapping("/{sessionId}/sensor-logs")
	fun sensorLogs(
		@PathVariable sessionId: String,
		@RequestParam(required = false, defaultValue = "2000") limit: Long,
	): Flux<SensorLogDocument> = sensorLogRepository.findBySessionIdOrderByTimeAsc(sessionId).take(limit)
}
