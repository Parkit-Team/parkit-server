package com.parkit.socket.dto

import io.swagger.v3.oas.annotations.media.Schema

/**
 * analysis-service에서 계산된 코칭 데이터
 */
@Schema(description = "장애물 거리 정보")
data class ObstacleDistancesDto(
	@field:Schema(description = "전방 거리")
	val frontDistance: Int,
	@field:Schema(description = "후방 거리")
	val backDistance: Int,
	@field:Schema(description = "좌측 거리")
	val leftDistance: Int,
	@field:Schema(description = "우측 거리")
	val rightDistance: Int,
)

@Schema(description = "코칭 메시지")
data class CoachingSocketDto(
	@field:Schema(description = "코칭 단계")
	val step: Int,
	@field:Schema(description = "타임스탬프(ms)")
	val timestamp: Long,
    @field:Schema(description = "목표 각도(deg)")
    val targetAngle: Int,
    @field:Schema(description = "목표 이동 거리(cm)")
    val targetDistance: Int,
    @field:Schema(description = "현재 각도(deg)")
    val currentAngle: Int,
    @field:Schema(description = "현재 이동 거리(cm)")
    val currentDistance: Int,
	@field:Schema(description = "장애물 거리")
	val distances: ObstacleDistancesDto,
	@field:Schema(description = "코칭 ID")
	val coachingId: Int,
)
