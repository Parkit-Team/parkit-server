package com.parkit.analysis.domain

/**
 * 실시간 센서 데이터를 분석하여 판단한 위험 수준
 */
enum class CoachingLevel {
    SAFE,       // 안전
    WARNING,    // 주의
    DANGER      // 위험
}
