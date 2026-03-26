package com.parkit.analysis.kafka

import com.fasterxml.jackson.databind.ObjectMapper
import com.parkit.analysis.coaching.dto.CoachingSocketDto
import com.parkit.analysis.kafka.dto.ParkingSensorDto
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.kafka.test.context.EmbeddedKafka
import org.springframework.stereotype.Component
import org.springframework.test.context.TestPropertySource
import java.io.File
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.CountDownLatch

@SpringBootTest
@EmbeddedKafka(partitions = 1, ports = [0], topics = ["sensor-topic", "coaching-event"])
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
			"../data/step1.csv",
			"../data/step2.csv",
			"../data/step3.csv",
			"../data/step4.csv"
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

			// 현재 스텝에 대한 수집 리스트 초기화/마크 전 대기 (이전 스텝 메시지 잔류 방지)
			Thread.sleep(500)
			val currentReceivedCountBefore = testConsumer.receivedEvents.size

			val lines = file.readLines()
			for (i in 1 until lines.size) { // 0번째 인덱스는 헤더이모로 생략
				val line = lines[i]
				if (line.isBlank()) continue

				val event = parseCsvLine(line)
				
				// 센서 이벤트 전송
				val jsonPayload = objectMapper.writeValueAsString(event)
				kafkaTemplate.send("sensor-topic", sessionId, jsonPayload)
				Thread.sleep(50) 
			}

			// then: 해당 파일의 모든 이벤트가 expectedStep으로 처리되었는지 검증
			// (마지막 이벤트는 transition을 발생시킬 수 있으므로 약간의 여유를 줌)
			Thread.sleep(1000) // 처리 대기

			val eventsSinceStart = testConsumer.receivedEvents.toList()
				.drop(currentReceivedCountBefore)
				.map { objectMapper.readValue(it, CoachingSocketDto::class.java) }

			assertTrue(eventsSinceStart.isNotEmpty(), "Step $expectedStep 에 대한 코칭 이벤트가 수집되어야 합니다.")
			
			eventsSinceStart.forEach { dto ->
				// 예외 상황: 전환 직후의 이벤트가 섞일 수 있으나, 기본적으로는 expectedStep이어야 함.
				// 만약 전환 지점에서 step이 변했다면, 해당 데이터 시나리오의 의도에 맞는지 확인.
				if (dto.step != expectedStep) {
					println("DEBUG: Step mismatch for file step$expectedStep.csv -> Received DTO step: ${dto.step}")
				}
				// 주차 시나리오 파일의 내용은 해당 스텝의 동작을 담고 있으므로assertEquals로 강제.
				// 단, 마지막 레코드에서 다음 스텝으로 넘어갔다면 그 다음 레코드는 다음 파일에 있어야 함.
				assertEquals(expectedStep, dto.step, "CSV $filePath 의 데이터는 Step $expectedStep 이어야 합니다.")
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
	private val logger = LoggerFactory.getLogger(javaClass)
	val latch = CountDownLatch(1)
	val receivedEvents = CopyOnWriteArrayList<String>()

	@KafkaListener(topics = ["coaching-event"], groupId = "test-group-coaching")
	fun consumeCoachingEvent(record: ConsumerRecord<String, String>) {
		receivedEvents.add(record.value())
		latch.countDown()
	}

}
