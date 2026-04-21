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
    val timestamp: String,
    // units: angle=deg, distance=m
    val targetAngle: Int,
    val targetDistance: Double,
    val currentAngle: Int,
    val currentDistance: Double,
	val distances: ObstacleDistancesDto,
    val coachingId: Int,
)
