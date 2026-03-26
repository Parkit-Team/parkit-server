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
    val initialX: Double? = null,
    val initialY: Double? = null,
    val message: String
)
