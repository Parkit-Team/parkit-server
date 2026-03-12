package com.parkit.socket.config

import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.info.Info
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class SwaggerConfig {

    @Bean
    fun openAPI(): OpenAPI {
        return OpenAPI()
            .info(
                Info()
                    .title("Parkit Socket Service API")
                    .description("Socket(WebSocket/STOMP) 서비스 문서")
                    .version("v1")
            )
    }
}
