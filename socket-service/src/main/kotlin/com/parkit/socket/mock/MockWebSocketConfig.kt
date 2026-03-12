package com.parkit.socket.mock

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.context.annotation.Configuration
import org.springframework.web.socket.config.annotation.StompEndpointRegistry
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer

@Configuration
@ConditionalOnProperty(prefix = "parkit.mock.coaching", name = ["enabled"], havingValue = "true")
class MockWebSocketConfig : WebSocketMessageBrokerConfigurer {
	override fun registerStompEndpoints(registry: StompEndpointRegistry) {
		registry.addEndpoint("/ws/parkit-mock")
			.setAllowedOriginPatterns("*")
			.withSockJS()
	}
}
