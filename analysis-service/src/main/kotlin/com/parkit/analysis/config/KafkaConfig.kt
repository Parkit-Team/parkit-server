package com.parkit.analysis.config

import org.springframework.context.annotation.Configuration
import org.springframework.kafka.annotation.EnableKafka

@EnableKafka
@Configuration
class KafkaConfig {
    // Spring Boot 자동 구성(AutoConfiguration)이 application.yml 설정을 바탕으로 
    // KafkaTemplate 및 ConcurrentKafkaListenerContainerFactory 등을 주입합니다.
}
