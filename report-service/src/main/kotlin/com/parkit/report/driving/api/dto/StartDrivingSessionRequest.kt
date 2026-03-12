package com.parkit.report.driving.api.dto

import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "주행 세션 시작 요청")
data class StartDrivingSessionRequest(
	@field:Schema(description = "사용자 ID(옵션)", example = "user-123")
	val userId: String? = null,
)
