package com.parkit.analysis.publisher

import com.parkit.analysis.dto.CoachingAlertEvent

/**
 * 코칭 이벤트 -> socket service로 전달하기 위한 인터페이스
 */
interface CoachingEventPublisher {
    fun publish(event: CoachingAlertEvent)
}
