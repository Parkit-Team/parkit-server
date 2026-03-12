package com.parkit.analysis.driving.persistence.document

import com.parkit.analysis.driving.domain.DrivingSessionStatus

import io.swagger.v3.oas.annotations.media.Schema
import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.mapping.Document
import java.time.Instant

@Document("driving_sessions")
@Schema(description = "MongoDB 주행 세션 문서")
data class DrivingSessionDocument(
	@Id
	@field:Schema(description = "주행 세션 ID")
	val id: String,
	@field:Schema(description = "사용자 ID(옵션)")
	val userId: String?,
	@field:Schema(description = "세션 상태")
	val status: DrivingSessionStatus,
	@field:Schema(description = "세션 시작 시간(UTC)")
	val startedAt: Instant,
	@field:Schema(description = "세션 종료 시간(UTC)")
	val stoppedAt: Instant?,
	@field:Schema(description = "프론트에서 계산한 점수")
	val frontendScore: Double?,
)
