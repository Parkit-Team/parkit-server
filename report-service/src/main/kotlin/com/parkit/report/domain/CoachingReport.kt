package com.parkit.report.domain

import jakarta.persistence.*
import java.time.LocalDateTime

@Entity
@Table(name = "coaching_reports")
class CoachingReport(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,

    @Column(nullable = false)
    val sessionId: String,

    @Column(nullable = false)
    val score: Int,

    @Column(nullable = false, length = 1000)
    val issues: String,

    @Column(nullable = false)
    val createdAt: LocalDateTime = LocalDateTime.now()
) {
    // 빈 생성자 및 클래스 속성은 Kotlin JPA plugin (allOpen, noArg) 등에 의해 처리됩니다.
}
