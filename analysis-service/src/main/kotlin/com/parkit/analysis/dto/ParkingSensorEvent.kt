package com.parkit.analysis.dto

/**
 * 센서 브릿지(Python)에서 Kafka를 통해 보내는 실시간 차량 센서 데이터를 표현하는 모델
 */
data class ParkingSensorEvent(
    val status: String,
    val msg: String
)
