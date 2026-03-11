package com.parkit.analysis.coaching.dto

/**
 * socket service로 전달될 코칭 알림 이벤트
 */
data class ObstacleDistancesDto(
	val frontDistance: Double,
	val backDistance: Double,
	val leftDistance: Double,
	val rightDistance: Double,
)

data class CoachingSocketDto(
    val step: Int,
    val timestamp: Long,
    val targetAngle: Double,
    val targetDistance: Double,
    val currentAngle: Double,
    val currentDistance: Double,
	val distances: ObstacleDistancesDto,
    val coachingId: Int,
)
