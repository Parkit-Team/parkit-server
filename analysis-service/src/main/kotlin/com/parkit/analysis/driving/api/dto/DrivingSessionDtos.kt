package com.parkit.analysis.driving.api.dto

import com.fasterxml.jackson.annotation.JsonProperty
import com.parkit.analysis.kafka.dto.ParkingSensorDto

data class StartDrivingSessionRequest(
	val userId: String? = null,
)

data class StartDrivingSessionResponse(
	val sessionId: String,
	val startedAt: java.time.Instant,
)

data class StopDrivingSessionRequest(
	val frontendScore: Double,
)

data class DrivingSensorLogRequest(
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
	@JsonProperty("rear_dist") val rearDist: Double,
) {
	fun toDto(): ParkingSensorDto = ParkingSensorDto(
		time = time,
		x = x,
		y = y,
		z = z,
		steer = steer,
		wheelDegree = wheelDegree,
		handleAngle = handleAngle,
		speed = speed,
		frontDist = frontDist,
		leftDist = leftDist,
		rightDist = rightDist,
		rearDist = rearDist,
	)
}
