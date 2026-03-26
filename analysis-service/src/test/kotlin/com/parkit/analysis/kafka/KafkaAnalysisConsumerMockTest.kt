package com.parkit.analysis.kafka

import com.fasterxml.jackson.databind.ObjectMapper
import com.parkit.analysis.coaching.dto.CoachingSocketDto
import com.parkit.analysis.kafka.dto.ParkingSensorDto
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
			"../data/step1.csv",
			"../data/step2.csv",
			"../data/step3.csv",
			"../data/step4.csv"
		)
		
		val sessionId = "test-session-mock"
		
		val jsonPayloadSlot = mutableListOf<String>()

		csvFiles.forEachIndexed { index, filePath ->
			val expectedStep = index + 1
			println("=== Step \$expectedStep Mock 시나리오 시작 (\$filePath) ===")
			
			val file = File(filePath)
			if (!file.exists()) {
				println("WARN: 파일이 존재하지 않습니다. 생략합니다. (\$filePath)")
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
				val event = parseCsvLine(line)
				val jsonPayload = objectMapper.writeValueAsString(event)
				
				// 핵심! Kafka 브로커를 거치지 않고 타겟 메서드에 직접 데이터 주입
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
			
			println("--- Step \$expectedStep 검증 완료 (\${eventsSinceStart.size} events) ---")
		}
	}

	private fun parseCsvLine(line: String): ParkingSensorDto {
		val tokens = line.split(",").map { it.trim() }
		return ParkingSensorDto(
			time = tokens[0].toDoubleOrNull() ?: 0.0,
			x = tokens[1].toDoubleOrNull() ?: 0.0,
			y = tokens[2].toDoubleOrNull() ?: 0.0,
			z = tokens[3].toDoubleOrNull() ?: 0.0,
			steer = tokens[4].toDoubleOrNull() ?: 0.0,
			wheelDegree = tokens[5].toDoubleOrNull() ?: 0.0,
			handleAngle = tokens[6].toDoubleOrNull() ?: 0.0,
			speed = if (tokens[7] == "nan") 0.0 else tokens[7].toDoubleOrNull() ?: 0.0,
			frontDist = tokens[8].toDoubleOrNull() ?: 0.0,
			leftDist = tokens[9].toDoubleOrNull() ?: 0.0,
			rightDist = tokens[10].toDoubleOrNull() ?: 0.0,
			rearDist = tokens[11].toDoubleOrNull() ?: 0.0
		)
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
