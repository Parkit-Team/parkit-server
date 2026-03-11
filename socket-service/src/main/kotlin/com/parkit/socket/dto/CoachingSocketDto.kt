package com.parkit.socket.dto

/**
 * analysis-service에서 계산된 코칭 데이터
 */
data class CoachingSocketDto(
	val step: Int,
	val timestamp: Long,
	val targetAngle: Double,
	val targetDistance: Double,
	val currentAngle: Double,
	val currentDistance: Double,
	val frontDistance: Double,
	val backDistance: Double,
	val leftDistance: Double,
	val rightDistance: Double,
	val coachingId: Int,
)
