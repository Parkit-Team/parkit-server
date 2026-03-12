package com.parkit.analysis.driving.api.dto

import com.fasterxml.jackson.annotation.JsonProperty
import com.parkit.analysis.kafka.dto.ParkingSensorDto
import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "주행 세션 시작 요청")
data class StartDrivingSessionRequest(
	@field:Schema(description = "사용자 ID(옵션)", example = "user-123")
	val userId: String? = null,
)

@Schema(description = "주행 세션 시작 응답")
data class StartDrivingSessionResponse(
	@field:Schema(description = "주행 세션 ID")
	val sessionId: String,
	@field:Schema(description = "세션 시작 시간(UTC)")
	val startedAt: java.time.Instant,
)

@Schema(description = "주행 세션 종료 요청")
data class StopDrivingSessionRequest(
	@field:Schema(description = "프론트에서 계산한 점수", example = "87.5")
	val frontendScore: Double,
)

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
