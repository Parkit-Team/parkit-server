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
    // units: angle=deg, distance=m
    val targetAngle: Int,
    val targetDistance: Double,
    val currentAngle: Int,
    val currentDistance: Double,
	val distances: ObstacleDistancesDto,
    val coachingId: Int,
)
