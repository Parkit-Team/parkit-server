package com.parkit.analysis.coaching.dto

/**
 * socket service로 전달될 코칭 알림 이벤트
 */
data class ObstacleDistancesDto(
	val frontDistance: Int,
	val backDistance: Int,
	val leftDistance: Int,
	val rightDistance: Int,
)

data class CoachingSocketDto(
    val step: Int,
    val timestamp: Long,
    // units: angle=deg, distance=cm
    val targetAngle: Int,
    val targetDistance: Int,
    val currentAngle: Int,
    val currentDistance: Int,
	val distances: ObstacleDistancesDto,
    val coachingId: Int,
)
