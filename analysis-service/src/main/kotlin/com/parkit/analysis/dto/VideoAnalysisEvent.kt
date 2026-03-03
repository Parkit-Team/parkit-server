package com.parkit.analysis.dto

/**
 * AI/비디오 영상 처리 모듈에서 Kafka를 통해 보내는 데이터를 표현하는 모델
 */
data class VideoAnalysisEvent(
    val videoId: String,
    val timestamp: Long,
    val boundingBox: List<Double>, // [x, y, w, h] 등
    val jointData: Map<String, List<Double>>, // 관절 자세 데이터 예: {"leftShoulder": [x, y, score]}
    val accuracy: Double, // 정확도 점수
    val eventType: String? = null // 이벤트 타입 예: "fall", "fire" 등
)
