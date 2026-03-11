package com.parkit.analysis.dto

import com.fasterxml.jackson.annotation.JsonProperty
/**
 * 센서 브릿지(Python)에서 Kafka를 통해 보내는 실시간 차량 센서 데이터를 표현하는 모델
 */
data class ParkingSensorDto(
    val time: Double,
    val x: Double,
    val y: Double,
    val z: Double,
    val steer: Double,
    @JsonProperty("wheel_degree") val wheelDegree: Double,
    @JsonProperty("handle_angle") val handleAngle: Double,
    val speed: Double,
    @JsonProperty("front_dist") val frontDist: Double,
    @JsonProperty("left_dist") val leftDist: Double,
    @JsonProperty("right_dist") val rightDist: Double,
    @JsonProperty("rear_dist") val rearDist: Double
)