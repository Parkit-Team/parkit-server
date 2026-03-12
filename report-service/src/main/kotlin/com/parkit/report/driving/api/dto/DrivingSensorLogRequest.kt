package com.parkit.report.driving.api.dto

import com.fasterxml.jackson.annotation.JsonProperty
import com.parkit.report.kafka.dto.ParkingSensorDto
import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "센서 로그 단건 저장 요청")
data class DrivingSensorLogRequest(
	@field:Schema(description = "센서 이벤트 time(원본 필드)")
	val time: Double,
	@field:Schema(description = "X 좌표")
	val x: Double,
	@field:Schema(description = "Y 좌표")
	val y: Double,
	@field:Schema(description = "Z 좌표")
	val z: Double,
	@field:Schema(description = "steer")
	val steer: Double,
	@field:Schema(description = "wheel_degree (snake_case 허용)")
	@JsonProperty("wheel_degree") val wheelDegree: Double,
	@field:Schema(description = "handle_angle (snake_case 허용)")
	@JsonProperty("handle_angle") val handleAngle: Double,
	@field:Schema(description = "속도")
	val speed: Double,
	@field:Schema(description = "front_dist (snake_case 허용)")
	@JsonProperty("front_dist") val frontDist: Double,
	@field:Schema(description = "left_dist (snake_case 허용)")
	@JsonProperty("left_dist") val leftDist: Double,
	@field:Schema(description = "right_dist (snake_case 허용)")
	@JsonProperty("right_dist") val rightDist: Double,
	@field:Schema(description = "rear_dist (snake_case 허용)")
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
