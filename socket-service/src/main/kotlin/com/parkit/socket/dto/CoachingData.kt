package com.parkit.socket.dto

data class CoachingData(
    val sensorId: String,
    val distance: Double,
    val warningLevel: String,
    val message: String,
    val timestamp: Long
)
