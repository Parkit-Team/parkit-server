package com.parkit.analysis.global.config

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Primary

@Configuration
class JacksonConfig {

    @Bean
    @Primary // 다른 ObjectMapper보다 이 설정을 우선적으로 사용하도록 설정
    fun objectMapper(): ObjectMapper {
        return jacksonObjectMapper()
            .registerModule(JavaTimeModule()) // 👈 Instant, LocalDateTime 등 Java 8 시간 타입 지원 추가
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false) // 모르는 프로퍼티가 있어도 에러 내지 않음
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS) // 👈 날짜를 숫자 배열 대신 ISO-8601 문자열로 저장
    }
}