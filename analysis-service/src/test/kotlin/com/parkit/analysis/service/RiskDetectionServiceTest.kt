package com.parkit.analysis.service

import com.parkit.analysis.domain.CoachingLevel
import com.parkit.analysis.dto.CoachingAlertEvent
import com.parkit.analysis.dto.ParkingSensorEvent
import com.parkit.analysis.publisher.CoachingEventPublisher
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.mockk.mockk
import io.mockk.verify

class RiskDetectionServiceTest : BehaviorSpec({

    val mockPublisher = mockk<CoachingEventPublisher>(relaxed = true)
    val riskDetectionService = RiskDetectionService(mockPublisher)

    Given("주차 센서 이벤트가 주어졌을 때") {
        When("거리가 2.0m (안전 거리)인 경우") {
            val event = ParkingSensorEvent(sensorId = 1, distance = 2.0, timestamp = System.currentTimeMillis())
            val result = riskDetectionService.detectRisk(event)

            Then("SAFE 레벨의 코칭 이벤트를 반환한다") {
                result shouldNotBe null
                result?.level shouldBe CoachingLevel.SAFE
                result?.message shouldBe "그대로 후진하세요"
                result?.sensorId shouldBe 1
            }

            Then("Publisher 의 publish 가 호출된다") {
                // processEvent 를 호출해야 publish 가 동작합니다
                riskDetectionService.processEvent(event)
                verify(exactly = 1) { mockPublisher.publish(any<CoachingAlertEvent>()) }
            }
        }

        When("거리가 0.8m (주의 거리)인 경우") {
            val event = ParkingSensorEvent(sensorId = 1, distance = 0.8, timestamp = System.currentTimeMillis())
            val result = riskDetectionService.detectRisk(event)

            Then("WARNING 레벨의 코칭 이벤트를 반환한다") {
                result shouldNotBe null
                result?.level shouldBe CoachingLevel.WARNING
                result?.message shouldBe "장애물이 가까워집니다. 속도를 줄이세요"
            }
        }

        When("거리가 0.2m (위험 거리)인 경우") {
            val event = ParkingSensorEvent(sensorId = 1, distance = 0.2, timestamp = System.currentTimeMillis())
            val result = riskDetectionService.detectRisk(event)

            Then("DANGER 레벨의 코칭 이벤트를 반환한다") {
                result shouldNotBe null
                result?.level shouldBe CoachingLevel.DANGER
                result?.message shouldBe "충돌 위험! 정지하세요"
            }
        }
    }
})
