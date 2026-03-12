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
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
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
@Tag(
	name = "주행 리포트",
	description = "주행 세션(start/stop), 센서 로그, 프론트 점수(frontendScore)를 저장/조회합니다.",
)
class DrivingSessionController(
	private val drivingSessionService: DrivingSessionService,
	private val sensorLogService: SensorLogService,
	private val sensorLogRepository: SensorLogMongoRepository,
) {
	@PostMapping("/start")
	@ResponseStatus(HttpStatus.CREATED)
	@Operation(
		summary = "주행 세션 시작",
		description = "클라이언트 start 요청으로 RUNNING 세션을 생성합니다. 이미 RUNNING 세션이 있으면 새로 만들지 않고 기존 세션을 반환합니다.",
	)
	@ApiResponses(
		ApiResponse(responseCode = "201", description = "세션 생성 또는 기존 RUNNING 세션 반환"),
	)
	fun start(@RequestBody(required = false) request: StartDrivingSessionRequest?): Mono<StartDrivingSessionResponse> =
		drivingSessionService.start(request?.userId)
			.map { StartDrivingSessionResponse(sessionId = it.id, startedAt = it.startedAt) }

	@PostMapping("/{sessionId}/stop")
	@Operation(
		summary = "주행 세션 종료",
		description = "세션을 STOPPED로 변경하고 프론트에서 계산한 점수(frontendScore)를 저장합니다.",
	)
	@ApiResponses(
		ApiResponse(responseCode = "200", description = "종료 처리 완료"),
		ApiResponse(responseCode = "404", description = "세션을 찾을 수 없음", content = [Content()]),
	)
	fun stop(
		@Parameter(description = "주행 세션 ID", required = true)
		@PathVariable sessionId: String,
		@RequestBody request: StopDrivingSessionRequest,
	): Mono<DrivingSessionDocument> = drivingSessionService.stop(sessionId, request.frontendScore)

	@PostMapping("/{sessionId}/sensor-logs")
	@ResponseStatus(HttpStatus.CREATED)
	@Operation(
		summary = "센서 로그 저장(HTTP)",
		description = "webot controller가 HTTP로 센서 데이터를 전달할 때 사용합니다. Kafka 수집을 사용하는 경우 호출할 필요가 없습니다.",
	)
	@ApiResponses(
		ApiResponse(responseCode = "201", description = "센서 로그 저장 완료"),
	)
	fun appendSensorLog(
		@Parameter(description = "주행 세션 ID", required = true)
		@PathVariable sessionId: String,
		@RequestBody request: DrivingSensorLogRequest,
	): Mono<SensorLogDocument> = sensorLogService.append(sessionId, request.toDto())

	@GetMapping("/{sessionId}")
	@Operation(summary = "주행 세션 조회")
	@ApiResponses(
		ApiResponse(responseCode = "200", description = "조회 성공"),
		ApiResponse(responseCode = "404", description = "세션을 찾을 수 없음", content = [Content()]),
	)
	fun get(@PathVariable sessionId: String): Mono<DrivingSessionDocument> = drivingSessionService.get(sessionId)

	@GetMapping("/{sessionId}/sensor-logs")
	@Operation(
		summary = "센서 로그 목록 조회",
		description = "세션의 센서 로그를 time 오름차순으로 조회합니다.",
	)
	fun sensorLogs(
		@Parameter(description = "주행 세션 ID", required = true)
		@PathVariable sessionId: String,
		@Parameter(description = "최대 반환 개수", example = "2000")
		@RequestParam(required = false, defaultValue = "2000") limit: Long,
	): Flux<SensorLogDocument> = sensorLogRepository.findBySessionIdOrderByTimeAsc(sessionId).take(limit)
}
