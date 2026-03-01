package com.parkit.socket.dto

/**
 * 실시간 코칭 데이터
 * @property sensorId 센서 ID
 * @property distance 거리
 * @property warningLevel 경고 레벨
 * @property message 메시지
 * @property timestamp 타임스탬프
 */
data class CoachingData(
    val sensorId: String,
    val distance: Double,
    val warningLevel: String,
    val message: String,
    val timestamp: Long
)
