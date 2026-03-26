package com.parkit.analysis.kafka

import com.fasterxml.jackson.databind.ObjectMapper
import com.parkit.analysis.coaching.dto.CoachingSocketDto
import com.parkit.analysis.kafka.dto.ParkingSensorDto
import com.parkit.analysis.util.TestCsvUtils
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Primary
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.test.context.TestPropertySource
import org.slf4j.LoggerFactory
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import io.mockk.clearMocks
import org.springframework.context.annotation.Bean
import java.io.File
import java.util.concurrent.CompletableFuture

@SpringBootTest
@TestPropertySource(properties = [
	"parkit.kafka.topics.sensor=sensor-topic",
	"parkit.kafka.topics.coachingEvent=coaching-event"
])
class KafkaAnalysisConsumerMockTest {

	private val log = LoggerFactory.getLogger(javaClass)

	@Autowired
	private lateinit var objectMapper: ObjectMapper

	// 실제 서비스 로직 수신자
	@Autowired
	private lateinit var kafkaAnalysisConsumer: KafkaAnalysisConsumer

	// TestConfig에서 주입해주는 가짜(MockK) KafkaTemplate 객체
	@Autowired
	private lateinit var kafkaTemplate: KafkaTemplate<String, String>

	@BeforeEach
	fun setup() {
		// kafkaTemplate.send 가 호출될 때 에러를 뱉지 않고 완료된 Future 를 반환하도록 설정
		every { kafkaTemplate.send(any(), any(), any<String>()) } returns CompletableFuture.completedFuture(null)
	}

	@Test
	fun `Mock 주차 단계별 CSV 시나리오 전송 및 서비스 로직 검증`() {
		val csvFiles = listOf(
			"src/test/resources/data/step01.csv",
			"src/test/resources/data/step02.csv",
			"src/test/resources/data/step03.csv",
			"src/test/resources/data/step04.csv"
		)
		
		val sessionId = "test-session-mock"
		
		val jsonPayloadSlot = mutableListOf<String>()

		csvFiles.forEachIndexed { index, filePath ->
			val expectedStep = index + 1
			log.info("=== Step {} Mock 시나리오 시작 ({}) ===", expectedStep, filePath)
			
			val file = File(filePath)
			if (!file.exists()) {
				log.warn("파일이 존재하지 않습니다. 생략합니다. ({})", filePath)
				return@forEachIndexed
			}

			val lines = file.readLines()
			val recordsToSend = lines.drop(1).filter { it.isNotBlank() }
			
			// 다음 파일을 처리하기 전에 Mock 객체의 기록과 캡처 초기화
			clearMocks(kafkaTemplate, answers = false) 
			jsonPayloadSlot.clear()

			// 전송 캡처 설정
			every { 
				kafkaTemplate.send("coaching-event", sessionId, capture(jsonPayloadSlot))
			} returns CompletableFuture.completedFuture(null)

			for (line in recordsToSend) {
				val event = TestCsvUtils.parseCsvLine(line)
				val jsonPayload = objectMapper.writeValueAsString(event)
				
				// Kafka 브로커를 거치지 않고 타겟 메서드에 직접 데이터 주입
				val record = ConsumerRecord("sensor-topic", 0, 0L, sessionId, jsonPayload)
				kafkaAnalysisConsumer.consume(record)
			}
			
			verify(exactly = recordsToSend.size) { 
				kafkaTemplate.send("coaching-event", sessionId, any<String>()) 
			}

			val eventsSinceStart = jsonPayloadSlot
				.map { objectMapper.readValue(it, CoachingSocketDto::class.java) }

			assertTrue(eventsSinceStart.isNotEmpty(), "Step \$expectedStep 에 대한 코칭 이벤트가 수집되어야 합니다.")
			
			eventsSinceStart.forEach { dto ->
				assertEquals(expectedStep, dto.step, "CSV \$filePath 의 데이터는 Step \$expectedStep 이어야 합니다. (DTO: \$dto)")
			}
			
			log.info("--- Step {} 검증 완료 ({} events) ---", expectedStep, eventsSinceStart.size)
		}
	}

	@TestConfiguration
	class TestConfig {
		@Bean
		@Primary
		fun kafkaTemplate(): KafkaTemplate<String, String> {
			// MockK를 이용한 가짜 KafkaTemplate 생성
			return mockk(relaxed = true)
		}
	}
}
