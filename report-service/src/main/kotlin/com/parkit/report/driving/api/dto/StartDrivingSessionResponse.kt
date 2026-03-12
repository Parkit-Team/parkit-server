package com.parkit.report.driving.api.dto

import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "주행 세션 시작 응답")
data class StartDrivingSessionResponse(
	@field:Schema(description = "주행 세션 ID")
	val sessionId: String,
	@field:Schema(description = "세션 시작 시간(UTC)")
	val startedAt: java.time.Instant,
)
