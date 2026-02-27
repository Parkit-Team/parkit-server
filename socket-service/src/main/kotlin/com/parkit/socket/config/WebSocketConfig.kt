package com.parkit.socket.config

import org.springframework.context.annotation.Configuration
import org.springframework.messaging.simp.config.MessageBrokerRegistry
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker
import org.springframework.web.socket.config.annotation.StompEndpointRegistry
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer

@Configuration
@EnableWebSocketMessageBroker
class WebSocketConfig : WebSocketMessageBrokerConfigurer {

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
    }
}
