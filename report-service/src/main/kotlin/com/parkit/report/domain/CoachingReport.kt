package com.parkit.report.domain

import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.mapping.Document
import java.time.LocalDateTime

@Document(collection = "coaching_reports")
class CoachingReport(
    @Id
    val id: String? = null,

    val sessionId: String,

    val score: Int,

    val issues: String,

    val createdAt: LocalDateTime = LocalDateTime.now()
) {
    // 빈 생성자 및 클래스 속성은 Kotlin JPA plugin (allOpen, noArg) 등에 의해 처리됩니다.
}
