package com.parkit.analysis.kafka.dto

import com.fasterxml.jackson.annotation.JsonProperty
/**
 * Kafka Producer -> broker -> Kafka Consumer
 * analysis-service는 consumer의 역할을 하므로 실시간 센서 데이터를 받는 DTO
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
