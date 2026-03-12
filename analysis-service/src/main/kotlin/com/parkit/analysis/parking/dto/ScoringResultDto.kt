package com.parkit.analysis.parking.dto

data class ScoringResultDto(
    val sessionId: String,
    val step: Int,
    val isCompleted: Boolean,
    val isCollision: Boolean,
    val errorX: Double?,
    val errorY: Double?,
    val errorZ: Double?,
    val errorHandle: Double?,
    val errorYaw: Double?,
    val trajectorySimilarityScore: Double?, // MSE 등 수치
    val scoreDeduction: Double,
    val message: String
)
