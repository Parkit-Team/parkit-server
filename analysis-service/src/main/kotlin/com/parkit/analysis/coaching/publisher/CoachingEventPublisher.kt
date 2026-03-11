package com.parkit.analysis.coaching.publisher

import com.parkit.analysis.coaching.dto.CoachingSocketDto

/**
 * 코칭 이벤트 -> socket service로 전달하기 위한 인터페이스
 */
interface CoachingEventPublisher {
    fun publish(event: CoachingSocketDto)
}