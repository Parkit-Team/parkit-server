package com.parkit.analysis.kafka

import com.fasterxml.jackson.databind.ObjectMapper
import com.parkit.analysis.coaching.dto.CoachingSocketDto
import com.parkit.analysis.kafka.dto.ParkingSensorDto
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.kafka.test.context.EmbeddedKafka
import org.springframework.stereotype.Component
import org.springframework.test.context.TestPropertySource
import org.awaitility.kotlin.await
import org.awaitility.kotlin.until
import java.io.File
import java.time.Duration
import java.util.concurrent.CopyOnWriteArrayList

import org.junit.jupiter.api.condition.DisabledIfEnvironmentVariable

@SpringBootTest
@EmbeddedKafka(partitions = 1, ports = [0], topics = ["sensor-topic", "coaching-event"])
// 젠킨스(CI) 또는 특정 환경(CI=true)일 때는 무거운 이 방식(EmbeddedKafka)을 건너뜁니다.
@DisabledIfEnvironmentVariable(named = "JENKINS_URL", matches = ".*")
@TestPropertySource(properties = [
	"spring.kafka.bootstrap-servers=\${spring.embedded.kafka.brokers}",
	"parkit.kafka.topics.sensor=sensor-topic",
	"parkit.kafka.topics.coachingEvent=coaching-event"
])
class KafkaAnalysisConsumerE2ETest {

	@Autowired
	private lateinit var kafkaTemplate: KafkaTemplate<String, String>

	@Autowired
	private lateinit var objectMapper: ObjectMapper

	@Autowired
	private lateinit var testConsumer: TestCoachingEventConsumer

	@Test
	fun `주차 단계별 CSV 시나리오 전송 및 처리 검증`() {
		// given: 각 스텝에 해당하는 CSV 파일 경로 목록
		val csvFiles = listOf(
			"src/test/resources/data/step01.csv",
			"src/test/resources/data/step02.csv",
			"src/test/resources/data/step03.csv",
			"src/test/resources/data/step04.csv"
		)
		
		val sessionId = "test-session-123"

		// when: CSV 파일을 순차적으로 읽어와 센서 데이터 전송
		csvFiles.forEachIndexed { index, filePath ->
			val expectedStep = index + 1
			println("=== Step $expectedStep 시나리오 시작 ($filePath) ===")
			
			val file = File(filePath)
			if (!file.exists()) {
				println("WARN: 파일이 존재하지 않습니다. 생략합니다. ($filePath)")
				return@forEachIndexed
			}

			// 현재까지 수집된 이벤트 수 확인
			val currentReceivedCountBefore = testConsumer.receivedEvents.size
			val lines = file.readLines()
			val recordsToSend = lines.drop(1).filter { it.isNotBlank() }

			for (line in recordsToSend) {
				if (line.isBlank()) continue

				val event = parseCsvLine(line)
				
				// 센서 이벤트 전송
				val jsonPayload = objectMapper.writeValueAsString(event)
				kafkaTemplate.send("sensor-topic", sessionId, jsonPayload)
				Thread.sleep(10) 
			}

			// then: 해당 파일의 모든 이벤트가 expectedStep으로 처리되었는지 검증
			// Awaitility를 사용하여 모든 레코드가 처리될 때까지 대기
			await.atMost(Duration.ofSeconds(10)).until {
				testConsumer.receivedEvents.size >= currentReceivedCountBefore + recordsToSend.size
			}

			val eventsSinceStart = testConsumer.receivedEvents.toList()
				.drop(currentReceivedCountBefore)
				.take(recordsToSend.size)
				.map { objectMapper.readValue(it, CoachingSocketDto::class.java) }

			assertTrue(eventsSinceStart.isNotEmpty(), "Step $expectedStep 에 대한 코칭 이벤트가 수집되어야 합니다.")
			
			eventsSinceStart.forEach { dto ->
				assertEquals(expectedStep, dto.step, "CSV $filePath 의 데이터는 Step $expectedStep 이어야 합니다. (DTO: $dto)")
			}
			
			println("--- Step $expectedStep 검증 완료 (${eventsSinceStart.size} events) ---")
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
}

/**
 * 테스트 용도로 coaching-event 토픽의 결과를 수집하는 Consumer
 */
@Component
class TestCoachingEventConsumer {
	val receivedEvents = CopyOnWriteArrayList<String>()

	@KafkaListener(topics = ["coaching-event"], groupId = "test-group-coaching")
	fun consumeCoachingEvent(record: ConsumerRecord<String, String>) {
		receivedEvents.add(record.value())
	}
}
