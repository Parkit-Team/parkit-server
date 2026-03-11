package com.parkit.analysis.coaching.publisher

import com.parkit.analysis.coaching.publisher.CoachingEventPublisher
import com.parkit.analysis.coaching.dto.CoachingSocketDto
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

/**
 * Kafka Producer 로직 연결되기 전까지 빌드/실행을 위한 임시 Bean 구현체
 */
@Component
class DummyCoachingEventPublisher : CoachingEventPublisher {

    private val log = LoggerFactory.getLogger(DummyCoachingEventPublisher::class.java)

	override fun publish(event: CoachingSocketDto) {
		log.info(
			"[Dummy Publisher] coaching event published - step: {}, targetAngle: {}, targetDistance: {}, currentAngle: {}, currentDistance: {}",
			event.step,
			event.targetAngle,
			event.targetDistance,
			event.currentAngle,
			event.currentDistance,
		)
	}
}