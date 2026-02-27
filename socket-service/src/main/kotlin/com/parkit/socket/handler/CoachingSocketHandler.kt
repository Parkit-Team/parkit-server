package com.parkit.socket.handler

import org.springframework.stereotype.Component
import org.springframework.web.reactive.socket.WebSocketHandler
import org.springframework.web.reactive.socket.WebSocketSession
import reactor.core.publisher.Mono

@Component
class CoachingSocketHandler : WebSocketHandler {

    override fun handle(session: WebSocketSession): Mono<Void> {
        println("새로운 웹소켓 연결 수립: \${session.id}")
        
        // TODO: Kafka Consumer 연동 시, 해당 Flux를 구독하여 클라이언트로 데이터를 전송하는 로직 구성 (session.send)
        val input = session.receive()
            .doOnNext { message -> println("수신된 메시지: \${message.payloadAsText}") }
            .then()
            
        return input
    }
}
