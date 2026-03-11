package com.parkit.analysis

import com.parkit.analysis.coaching.publisher.CoachingEventPublisher
import com.parkit.analysis.coaching.dto.CoachingSocketDto
import org.junit.jupiter.api.Test
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean

@SpringBootTest
class AnalysisServiceApplicationTests {

	@TestConfiguration
	class TestConfig {
		@Bean
		fun coachingEventPublisher(): CoachingEventPublisher {
			return object : CoachingEventPublisher {
				override fun publish(event: CoachingSocketDto) {
					// Dummy implementation for context loading
				}
			}
		}
	}

	@Test
	fun contextLoads() {
	}

}
