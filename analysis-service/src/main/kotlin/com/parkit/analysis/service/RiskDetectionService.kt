package com.parkit.analysis.service

import com.parkit.analysis.dto.VideoAnalysisEvent
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
class RiskDetectionService {
    private val log = LoggerFactory.getLogger(RiskDetectionService::class.java)

    /**
     * Kafka에서 수신한 VideoAnalysisEvent를 처리하여 실시간 위험을 감지합니다.
     */
    fun processEvent(event: VideoAnalysisEvent) {
        log.info("Processing VideoAnalysisEvent for videoId: ${event.videoId}, accuracy: ${event.accuracy}")
        // TODO: 실시간 위험 감지 알고리즘 로직 구현 예정
    }
}
