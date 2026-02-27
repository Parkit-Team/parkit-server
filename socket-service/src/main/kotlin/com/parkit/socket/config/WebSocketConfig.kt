package com.parkit.socket.config

import com.parkit.socket.handler.CoachingSocketHandler
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.web.reactive.HandlerMapping
import org.springframework.web.reactive.handler.SimpleUrlHandlerMapping
import org.springframework.web.reactive.socket.WebSocketHandler
import org.springframework.web.reactive.socket.server.support.WebSocketHandlerAdapter

@Configuration
class WebSocketConfig(
    private val coachingSocketHandler: CoachingSocketHandler
) {

    @Bean
    fun webSocketMapping(): HandlerMapping {
        val map = mapOf<String, WebSocketHandler>("/ws/coaching" to coachingSocketHandler)
        val mapping = SimpleUrlHandlerMapping()
        mapping.order = 1
        mapping.urlMap = map
        return mapping
    }

    @Bean
    fun handlerAdapter(): WebSocketHandlerAdapter {
        return WebSocketHandlerAdapter()
    }
}
