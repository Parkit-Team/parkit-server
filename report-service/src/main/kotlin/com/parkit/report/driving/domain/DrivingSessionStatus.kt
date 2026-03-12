package com.parkit.report.driving.domain

import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "주행 세션 상태")
enum class DrivingSessionStatus {
	RUNNING,
	STOPPED,
}
