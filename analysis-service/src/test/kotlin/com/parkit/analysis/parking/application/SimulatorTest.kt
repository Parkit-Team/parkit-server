package com.parkit.analysis.parking.application

import com.parkit.analysis.parking.repository.ParkingStepStateRepository
import com.parkit.analysis.parking.domain.ParkingEvent
import com.parkit.analysis.parking.domain.ParkingStepState
import com.parkit.analysis.parking.service.ParkingScoringService
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Test
import reactor.core.publisher.Mono
import java.io.BufferedReader

class SimulatorTest {

	@Test
	fun `실제 CSV 데이터를 활용한 시뮬레이션 테스트`() {
		val sessionId = "test-session"
		
		// Redis Mocking (메모리로 상태 유지)
		val stateMap = mutableMapOf<String, ParkingStepState>()
		val stateRepository = mockk<ParkingStepStateRepository>()
		
		every { stateRepository.findById(sessionId) } answers {
			val state = stateMap[sessionId]
			if (state != null) Mono.just(state) else Mono.empty()
		}
		
		every { stateRepository.save(any()) } answers {
			val state = firstArg<ParkingStepState>()
			stateMap[state.sessionId] = state
			Mono.just(true)
		}

		val scoringService = ParkingScoringService(stateRepository)

		println("=== T-Parking Simulator Started ===")
		val inputStream = javaClass.classLoader.getResourceAsStream("data/step01.csv")
			?: throw IllegalStateException("Missing test resource: data/step01.csv")
		val lines = inputStream.bufferedReader().use(BufferedReader::readLines).drop(1) // Header 제외
		
		for (line in lines) {
			val tokens = line.split(",")
			if (tokens.size < 12) continue
			
			// CSV 헤더: time, x, y, z, steer, wheel_degree, handle_angle, speed, front_dist, left_dist, right_dist, rear_dist
			val x = tokens[1].trim().toDoubleOrNull() ?: continue
			val y = tokens[2].trim().toDoubleOrNull() ?: continue
			val z = tokens[3].trim().toDoubleOrNull() ?: continue
			val handleAngle = tokens[6].trim().toDoubleOrNull() ?: continue
			val speed = tokens[7].trim().toDoubleOrNull() ?: 0.0
			val frontDist = tokens[8].trim().toDoubleOrNull() ?: 5.0
			val leftDist = tokens[9].trim().toDoubleOrNull() ?: 5.0
			val rightDist = tokens[10].trim().toDoubleOrNull() ?: 5.0
			val rearDist = tokens[11].trim().toDoubleOrNull() ?: 5.0

			val event = ParkingEvent(
				time = tokens[0].toDoubleOrNull() ?: 0.0,
				x = x, y = y, z = z, handleAngle = handleAngle,
				sensor = ParkingEvent.SensorData(frontDist, leftDist, rightDist, rearDist, speed)
			)

			// 처리
			val result = scoringService.processParkingEvent(sessionId, event).block()!!
			
			val isStepUp = result.message.contains("Evaluated")
			if (isStepUp || result.isCollision) {
				println("--------------------------------------------------")
				println("[EVENT] ${result.message}")
				println("Step: ${result.step}")
				if (isStepUp) {
					println("Errors -> X:${result.errorX}, Y:${result.errorY}, Z:${result.errorZ}, Handle:${result.errorHandle}, Yaw:${result.errorYaw}")
				}
				println("--------------------------------------------------")
			}
		}
		println("=== T-Parking Simulator Ended ===")
	}
}
