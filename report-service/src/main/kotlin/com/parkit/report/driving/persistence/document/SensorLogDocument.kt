package com.parkit.report.driving.persistence.document

import io.swagger.v3.oas.annotations.media.Schema
import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.index.CompoundIndex
import org.springframework.data.mongodb.core.index.CompoundIndexes
import org.springframework.data.mongodb.core.mapping.Document
import java.time.Instant

@Document("sensor_logs")
@CompoundIndexes(
	CompoundIndex(name = "session_time_idx", def = "{'sessionId': 1, 'time': 1}"),
)
@Schema(description = "MongoDB 센서 로그 문서")
data class SensorLogDocument(
	@Id
	@field:Schema(description = "센서 로그 ID")
	val id: String? = null,
	@field:Schema(description = "주행 세션 ID")
	val sessionId: String,
	@field:Schema(description = "프론트에서 계산한 점수(세션 종료 시 일괄 반영)")
	val frontendScore: Double? = null,
	@field:Schema(description = "서버 수신 시간(UTC)")
	val receivedAt: Instant,
	@field:Schema(description = "센서 이벤트 time(원본 필드)")
	val time: Double,
	val x: Double,
	val y: Double,
	val z: Double,
	val steer: Double,
	val wheelDegree: Double,
	val handleAngle: Double,
	val speed: Double,
	val frontDist: Double,
	val leftDist: Double,
	val rightDist: Double,
	val rearDist: Double,
)
