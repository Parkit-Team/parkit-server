package com.parkit.analysis.parking.application

import com.parkit.analysis.parking.repository.ParkingStepStateRepository
import com.parkit.analysis.parking.domain.Coordinate
import com.parkit.analysis.parking.domain.ParkingEvent
import com.parkit.analysis.parking.domain.ParkingStepState
import com.parkit.analysis.parking.service.ParkingScoringService
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import reactor.core.publisher.Mono
import reactor.test.StepVerifier

class ParkingScoringServiceTest {

	private lateinit var stateRepository: ParkingStepStateRepository
	private lateinit var scoringService: ParkingScoringService

    @BeforeEach
	fun setUp() {
		stateRepository = mockk()
		scoringService = ParkingScoringService(stateRepository)
	}

	private fun createDefaultSensorData() = ParkingEvent.SensorData(
        frontDistance = 5.0,
        leftDistance = 5.0,
        rightDistance = 5.0,
        rearDistance = 5.0,
        speed = 1.0
    )

    @Test
    fun `실시간 충돌 발생 시 실격(Collision) 결과를 반환해야 한다`() {
        val sessionId = "session-1"
        val state = ParkingStepState(sessionId)
        
        every { stateRepository.findById(sessionId) } returns Mono.just(state)
        every { stateRepository.save(any()) } returns Mono.just(true)

		val event = ParkingEvent(
			x = 0.0, y = 0.0, z = 0.0,
			handleAngle = 0.0,
			sensor = ParkingEvent.SensorData(
                frontDistance = 0.1,
                leftDistance = 5.0,
                rightDistance = 5.0,
                rearDistance = 5.0,
                speed = 1.0
            )
		)

        StepVerifier.create(scoringService.processParkingEvent(sessionId, event))
            .assertNext { result ->
                assertTrue(result.isCollision)
                assertTrue(result.isCompleted) // 충돌 시 종료
                assertEquals(1, result.step)
                assertEquals(100.0, result.scoreDeduction) // 실격 감점
            }
            .verifyComplete()
    }

    @Test
    fun `Step 1 진행 중인 경우, Max 조향각이 540에 도달하더라도 핸들이 0이 아니면 Step은 승격되지 않는다`() {
        val sessionId = "session-2"
        val state = ParkingStepState(sessionId)
        
        every { stateRepository.findById(sessionId) } returns Mono.just(state)
        every { stateRepository.save(any()) } returns Mono.just(true)

        // 강하게 돌림 (-536)
		val eventTurn = ParkingEvent(
			x = 0.405, y = 0.924, z = -0.08,
			handleAngle = -536.29,
			sensor = createDefaultSensorData()
		)

        StepVerifier.create(scoringService.processParkingEvent(sessionId, eventTurn))
            .assertNext { result ->
                assertFalse(result.isCompleted)
                assertFalse(result.isCollision)
                assertEquals(1, result.step)
                assertEquals(536.29, state.maxAbsHandleAngleInStep) // 보관됨
            }
            .verifyComplete()
    }

    @Test
    fun `Step 1 종료 조건 충족 시(Max Handle 540+ & 현재 0), Step 2로 승격되며 오차를 채점한다`() {
        val sessionId = "session-3"
        // 이미 핸들을 540도 돌린 적이 있는 상태라고 가정
        val state = ParkingStepState(sessionId, maxAbsHandleAngleInStep = 540.0)
        state.trajectory.add(Coordinate(0.0, 0.0)) // 시작점
        
        every { stateRepository.findById(sessionId) } returns Mono.just(state)
        every { stateRepository.save(any()) } returns Mono.just(true)

        // Step 1의 종료 목표: (7.568, 0.661)
		val eventFinish = ParkingEvent(
			x = 7.568, y = 0.661, z = -0.07,
			handleAngle = 0.0,
			sensor = createDefaultSensorData()
		)

        StepVerifier.create(scoringService.processParkingEvent(sessionId, eventFinish))
            .assertNext { result ->
                assertNotNull(result.errorX, "평가 되었으므로 오차가 null이 아니어야 함")
                assertEquals(0.0, result.errorX!!, 0.001)
                assertEquals(0.0, result.errorY!!, 0.001)
                
                // Step 2 상태로 내부 갱신되었는지 간접 확인 (현재 진행도 모델 상으로는, Event가 반영된 직후 Advance)
                assertEquals(1, result.step) // 결과 DTO는 해당 Step(1)에 대한 보고서
                assertEquals(0.0, result.scoreDeduction) // 오차가 0이므로 감점 없음
            }
            .verifyComplete()
        
        // Save 로직을 통해 state의 현재 step이 2로 올랐는지 확인
        assertEquals(2, state.currentStep)
        assertEquals(0.0, state.maxAbsHandleAngleInStep) // 초기화 확인
    }
}
