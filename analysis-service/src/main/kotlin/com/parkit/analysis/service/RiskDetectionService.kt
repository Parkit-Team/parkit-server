package com.parkit.analysis.service

import com.parkit.analysis.dto.ParkingSensorEvent
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
class RiskDetectionService {
    private val log = LoggerFactory.getLogger(RiskDetectionService::class.java)

    /**
     * Kafka에서 수신한 ParkingSensorEvent를 처리하여 실시간 위험을 감지합니다.
     */
    fun processEvent(event: ParkingSensorEvent) {
        log.info("Processing ParkingSensorEvent for sensorId: {}, distance: {}", event.sensorId, event.distance)
        // TODO: 실시간 위험 감지 알고리즘 로직 구현 예정
    }
}
