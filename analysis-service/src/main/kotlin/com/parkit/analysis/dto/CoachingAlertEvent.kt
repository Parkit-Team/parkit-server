package com.parkit.analysis.dto

import com.parkit.analysis.domain.CoachingLevel

/**
 * 3팀(클라이언트) 쪽에 WebSocket으로 전달될 정보
 */
data class CoachingAlertEvent(
    val sensorId: Int,
    val level: CoachingLevel,
    val message: String,
    val timestamp: Long
)
