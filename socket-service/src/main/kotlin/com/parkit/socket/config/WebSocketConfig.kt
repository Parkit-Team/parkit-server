package com.parkit.socket.config

import org.springframework.context.annotation.Configuration
import org.springframework.messaging.simp.config.MessageBrokerRegistry
import org.springframework.beans.factory.annotation.Value
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker
import org.springframework.web.socket.config.annotation.StompEndpointRegistry
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer

@Configuration
@EnableWebSocketMessageBroker
class WebSocketConfig : WebSocketMessageBrokerConfigurer {
	@Value("\${parkit.ws.raw.enabled:false}")
	private val rawWsEnabled: Boolean = false


    override fun configureMessageBroker(config: MessageBrokerRegistry) {
        // 서버 -> 클라이언트로 메시지를 보낼 때 사용할 prefix (주로 /topic, /queue)
        config.enableSimpleBroker("/topic")
        
        // 클라이언트 -> 서버로 메시지를 보낼 때 사용할 prefix
        config.setApplicationDestinationPrefixes("/app")
    }

	override fun registerStompEndpoints(registry: StompEndpointRegistry) {
		// 클라이언트가 연결할 STOMP 엔드포인트: /ws/parkit
		registry.addEndpoint("/ws/parkit")
			.setAllowedOriginPatterns("*") // 실제 운영 시에는 프론트엔드 도메인으로 제한
			.withSockJS() // SockJS 폴백 옵션 활성화

		// k6 같은 부하 도구는 SockJS 프로토콜 구현이 번거로워서, raw WebSocket 엔드포인트를 옵션으로 제공
		if (rawWsEnabled) {
			registry.addEndpoint("/ws/parkit-raw")
				.setAllowedOriginPatterns("*")
		}
	}
}
