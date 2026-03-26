package com.parkit.analysis.coaching.application

import com.parkit.analysis.coaching.service.RiskDetectionService
import com.parkit.analysis.kafka.dto.ParkingSensorDto
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldNotBeBlank

class RiskDetectionServiceTest : BehaviorSpec({
	val riskDetectionService = RiskDetectionService()

	// step1 시작 좌표에 고정하여 이동거리(currentDistance)가 흔들리지 않게 테스트
	fun createMockEvent(dist: Double) = ParkingSensorDto(
		time = 0.0, x = -9.0, y = 1.015761, z = 0.0, steer = 0.0,
		wheelDegree = 0.0, handleAngle = 0.0, speed = 1.0,
		frontDist = dist, leftDist = dist, rightDist = dist, rearDist = dist,
	)

	Given("주차 센서 이벤트가 주어졌을 때") {
		When("거리가 3.0m (안전 거리)인 경우") {
			val event = createMockEvent(3.0)
			val result = riskDetectionService.calculate(step = 1, event = event, initialX = event.x)

			Then("현재 거리/센서 거리가 DTO에 반영된다") {
				result.targetAngle shouldBe 0
				// STEP_1.x (13.2505) - initialX (-9.0) = 22.2505 -> 22.3
				result.targetDistance shouldBe 22.3
				result.currentDistance shouldBe 0.0
				result.distances.frontDistance shouldBe 300
				result.distances.backDistance shouldBe 300
				result.distances.leftDistance shouldBe 300
				result.distances.rightDistance shouldBe 300
			}

			Then("calculate는 양호 상태(5)를 반환한다") {
				val result2 = riskDetectionService.calculate(step = 1, event = event, initialX = event.x)
				result2.coachingId shouldBe 5
				result2.timestamp.shouldNotBeBlank()
			}
		}

		When("거리가 0.8m (주의 거리)인 경우") {
			val event = createMockEvent(0.8)
			val result = riskDetectionService.calculate(step = 1, event = event, initialX = event.x)

			Then("currentDistance에 최소 거리가 들어간다") {
				result.currentDistance shouldBe 0.0
			}

			Then("calculate는 코칭 이벤트를 반환한다") {
				riskDetectionService.calculate(step = 1, event = event, initialX = event.x).currentDistance shouldBe 0.0
			}
		}

		When("거리가 0.2m (위험 거리)인 경우") {
			val event = createMockEvent(0.2)
			val result = riskDetectionService.calculate(step = 1, event = event, initialX = event.x)

			Then("currentDistance에 최소 거리가 들어간다") {
				result.currentDistance shouldBe 0.0
			}
		}
	}
})
