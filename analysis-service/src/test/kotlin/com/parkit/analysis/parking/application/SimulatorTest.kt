package com.parkit.analysis.parking.application

import com.parkit.analysis.parking.repository.ParkingStepStateRepository
import com.parkit.analysis.parking.domain.ParkingEvent
import com.parkit.analysis.parking.domain.ParkingStepState
import com.parkit.analysis.parking.service.ParkingScoringService
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Test
import reactor.core.publisher.Mono
import java.io.File

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

        // CSV 읽기 (주의: 상대 경로는 프로젝트 루트 기준)
        val csvFile = File("../data/step01.csv")
        if (!csvFile.exists()) {
            // gradlew 실행 경로에 따라 다를 수 있음
            val altFile = File("data/step01.csv")
            if (altFile.exists()) csvFile.absolutePath else return
        }
        val targetFile = if (File("../data/step01.csv").exists()) File("../data/step01.csv") else File("../../data/step01.csv")

        println("=== T-Parking Simulator Started ===")
        val lines = targetFile.readLines().drop(1) // Header 제외
        
        for (line in lines) {
            val tokens = line.split(",")
            if (tokens.size < 12) continue
            
            // CSV 헤더: time, x, y, z, steer, wheel_degree, handle_angle, speed, front_dist, left_dist, right_dist, rear_dist
            val x = tokens[1].toDoubleOrNull() ?: continue
            val y = tokens[2].toDoubleOrNull() ?: continue
            val z = tokens[3].toDoubleOrNull() ?: continue
            val handleAngle = tokens[6].toDoubleOrNull() ?: continue
            val speed = tokens[7].toDoubleOrNull() ?: 0.0
            val frontDist = tokens[8].toDoubleOrNull() ?: 5.0
            val leftDist = tokens[9].toDoubleOrNull() ?: 5.0
            val rightDist = tokens[10].toDoubleOrNull() ?: 5.0
            val rearDist = tokens[11].toDoubleOrNull() ?: 5.0

			val event = ParkingEvent(
				x = x, y = y, z = z, handleAngle = handleAngle,
				sensor = ParkingEvent.SensorData(frontDist, leftDist, rightDist, rearDist, speed)
			)

            // 처리
            val result = scoringService.processParkingEvent(sessionId, event).block()!!
            
            val isStepUp = result.message.contains("Evaluated")
            if (isStepUp || result.isCollision) {
                println("--------------------------------------------------")
                println("[EVENT] ${result.message}")
                println("Step: ${result.step} | Deduction: ${result.scoreDeduction} | ScoreLeft: ${stateMap[sessionId]?.totalScore}")
                if (isStepUp) {
                    println("Errors -> X:${result.errorX}, Y:${result.errorY}, Z:${result.errorZ}, Handle:${result.errorHandle}, Yaw:${result.errorYaw}")
                }
                println("--------------------------------------------------")
            }
        }
        println("=== T-Parking Simulator Ended ===")
    }
}
