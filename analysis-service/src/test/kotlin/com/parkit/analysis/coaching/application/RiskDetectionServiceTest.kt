package com.parkit.analysis.coaching.application

import com.parkit.analysis.coaching.publisher.CoachingEventPublisher
import com.parkit.analysis.coaching.dto.CoachingSocketDto
import com.parkit.analysis.coaching.service.RiskDetectionService
import com.parkit.analysis.kafka.dto.ParkingSensorDto
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.mockk.mockk
import io.mockk.verify

class RiskDetectionServiceTest : BehaviorSpec({

    val mockPublisher = mockk<CoachingEventPublisher>(relaxed = true)
    val riskDetectionService = RiskDetectionService(mockPublisher)

    fun createMockEvent(dist: Double) = ParkingSensorDto(
        time = 0.0, x = 0.0, y = 0.0, z = 0.0, steer = 0.0,
        wheelDegree = 0.0, handleAngle = 0.0, speed = 1.0,
        frontDist = dist, leftDist = dist, rightDist = dist, rearDist = dist
    )

	Given("주차 센서 이벤트가 주어졌을 때") {
		When("거리가 2.0m (안전 거리)인 경우") {
			val event = createMockEvent(2.0)
			val result = riskDetectionService.createCoachingEvent(step = 1, event = event)

			Then("현재 거리/센서 거리가 DTO에 반영된다") {
				result.currentDistance shouldBe 2.0
				result.frontDistance shouldBe 2.0
				result.backDistance shouldBe 2.0
				result.leftDistance shouldBe 2.0
				result.rightDistance shouldBe 2.0
			}

			Then("publish는 호출되지 않는다 (안전 거리)") {
				riskDetectionService.processEvent(step = 1, event = event)
				verify(exactly = 0) { mockPublisher.publish(any<CoachingSocketDto>()) }
			}
		}

		When("거리가 0.8m (주의 거리)인 경우") {
			val event = createMockEvent(0.8)
			val result = riskDetectionService.createCoachingEvent(step = 1, event = event)

			Then("currentDistance에 최소 거리가 들어간다") {
				result.currentDistance shouldBe 0.8
			}

			Then("publish가 호출된다") {
				riskDetectionService.processEvent(step = 1, event = event)
				verify(exactly = 1) { mockPublisher.publish(any<CoachingSocketDto>()) }
			}
		}

		When("거리가 0.2m (위험 거리)인 경우") {
			val event = createMockEvent(0.2)
			val result = riskDetectionService.createCoachingEvent(step = 1, event = event)

			Then("currentDistance에 최소 거리가 들어간다") {
				result.currentDistance shouldBe 0.2
			}
		}
	}
})
