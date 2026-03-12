package com.parkit.socket.api

import com.parkit.socket.dto.CoachingSocketDto
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/socket")
@Tag(
	name = "웹소켓",
	description = "Swagger(OpenAPI)는 WebSocket(STOMP) 메시지 채널을 자동으로 문서화하지 못합니다. 대신 연결 엔드포인트와 토픽/메시지 스키마를 안내하는 HTTP 문서 엔드포인트를 제공합니다.",
)
class SocketDocsController {
	@GetMapping("/ws-info")
	@Operation(
		summary = "WebSocket(STOMP) 사용 안내",
		description = "클라이언트가 연결해야 하는 STOMP 엔드포인트(/ws/parkit)와 subscribe/send destination 규칙을 반환합니다.",
	)
	fun wsInfo(): WebSocketInfoResponse = WebSocketInfoResponse(
		stompEndpoints = listOf("/ws/parkit", "/ws/parkit-mock"),
		applicationDestinationPrefix = "/app",
		subscribeDestinations = listOf("/topic/coaching", "/topic/coaching-mock"),
		messageSchemas = listOf(
			MessageSchema(
				name = "CoachingSocketDto",
				description = "서버가 /topic/coaching 으로 브로드캐스트하는 코칭 메시지",
				javaType = CoachingSocketDto::class.qualifiedName ?: "com.parkit.socket.dto.CoachingSocketDto",
			),
		),
	)
}

@Schema(description = "WebSocket(STOMP) 사용 안내 응답")
data class WebSocketInfoResponse(
	@field:Schema(description = "SockJS/STOMP 엔드포인트 목록")
	val stompEndpoints: List<String>,
	@field:Schema(description = "클라이언트->서버 send prefix")
	val applicationDestinationPrefix: String,
	@field:Schema(description = "서버->클라이언트 subscribe destination 예시")
	val subscribeDestinations: List<String>,
	@field:Schema(description = "주요 메시지 스키마 목록")
	val messageSchemas: List<MessageSchema>,
)

@Schema(description = "메시지 스키마 메타 정보")
data class MessageSchema(
	@field:Schema(description = "스키마 이름")
	val name: String,
	@field:Schema(description = "설명")
	val description: String,
	@field:Schema(description = "Java/Kotlin 타입 FQN")
	val javaType: String,
)
