package com.parkit.report.driving.api.dto

import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "주행 세션 종료 요청")
data class StopDrivingSessionRequest(
	@field:Schema(description = "프론트에서 계산한 점수", example = "87.5")
	val frontendScore: Double,
)
