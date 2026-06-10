package com.hwcompany.fortune_index.domain.model

import com.hwcompany.fortune_index.common.SeoulTime
import com.hwcompany.fortune_index.consulting.AnalysisMode
import com.hwcompany.fortune_index.consulting.ConsultingScenario
import jakarta.persistence.Column
import jakarta.persistence.Embedded
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import java.time.LocalDateTime

@Entity
@Table(name = "consulting_histories")
data class ConsultingHistory(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    var user: User,

    @Enumerated(EnumType.STRING)
    @Column(name = "analysis_mode", nullable = false, length = 30)
    var analysisMode: AnalysisMode = AnalysisMode.INVESTMENT_ALL,

    @Enumerated(EnumType.STRING)
    @Column(name = "consulting_scenario", length = 30)
    var scenario: ConsultingScenario? = null,

    @Column(nullable = false)
    var consultedAt: LocalDateTime = SeoulTime.now(),

    @Embedded
    var tarotSnapshot: TarotHistorySnapshot,

    @Column(name = "question", columnDefinition = "TEXT")
    var question: String? = null,

    @Column(name = "analysis_result_json", nullable = false, columnDefinition = "TEXT")
    var analysisResultJson: String = "{}",

    @Column(name = "ai_response_json", nullable = false, columnDefinition = "TEXT")
    var aiResponseJson: String = "{}",

    @Column(name = "ai_safety_guard_applied", nullable = false)
    var aiSafetyGuardApplied: Boolean = false,

    @Column(name = "ai_safety_guard_reason", length = 200)
    var aiSafetyGuardReason: String? = null,

    @Column(name = "ai_safety_guard_matched_rules", length = 1000)
    var aiSafetyGuardMatchedRules: String? = null,

    @Column(name = "ai_safety_guard_original_text", columnDefinition = "TEXT")
    var aiSafetyGuardOriginalText: String? = null,

    @Column(name = "ai_safety_guard_sanitized_text", columnDefinition = "TEXT")
    var aiSafetyGuardSanitizedText: String? = null,

    @Column(name = "liked_at")
    var likedAt: LocalDateTime? = null
)
