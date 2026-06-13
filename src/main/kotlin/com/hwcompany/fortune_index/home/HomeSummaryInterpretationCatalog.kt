package com.hwcompany.fortune_index.home

import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.ObjectMapper
import com.hwcompany.fortune_index.common.SeoulTime
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.LocalDateTime
import org.slf4j.LoggerFactory
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

enum class HomeSummaryInterpretationCategory {
    SAJU_DAY,
    TAROT_CARD,
    ZODIAC_MOON
}

@Entity
@Table(name = "home_summary_interpretations")
data class HomeSummaryInterpretationEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    var category: HomeSummaryInterpretationCategory,

    @Column(nullable = false, length = 80)
    var code: String,

    @Column(nullable = false, length = 80)
    var variant: String = DEFAULT_HOME_SUMMARY_VARIANT,

    @Column(nullable = false, length = 120)
    var title: String,

    @Column(name = "daily_body", nullable = false, columnDefinition = "TEXT")
    var dailyBody: String,

    @Column(name = "personal_body_template", columnDefinition = "TEXT")
    var personalBodyTemplate: String? = null,

    @Column(name = "points_json", nullable = false, columnDefinition = "TEXT")
    var pointsJson: String,

    @Column(nullable = false)
    var active: Boolean = true,

    @Column(name = "created_at", nullable = false)
    var createdAt: LocalDateTime = SeoulTime.now(),

    @Column(name = "updated_at", nullable = false)
    var updatedAt: LocalDateTime = SeoulTime.now()
)

interface HomeSummaryInterpretationRepository : JpaRepository<HomeSummaryInterpretationEntity, Long> {
    fun findByCategoryAndCodeAndVariantAndActiveTrue(
        category: HomeSummaryInterpretationCategory,
        code: String,
        variant: String
    ): HomeSummaryInterpretationEntity?
}

data class ReviewedHomeSummaryInterpretation(
    val dailyBody: String,
    val personalBodyTemplate: String?,
    val points: List<String>?
)

@Service
class HomeSummaryInterpretationService(
    private val repository: HomeSummaryInterpretationRepository,
    private val objectMapper: ObjectMapper
) {
    @Transactional(readOnly = true)
    fun findReviewedContent(
        category: HomeSummaryInterpretationCategory,
        code: String,
        variant: String?
    ): ReviewedHomeSummaryInterpretation? {
        val candidates = listOfNotNull(
            variant?.trim()?.uppercase()?.takeIf { it.isNotBlank() && it != DEFAULT_HOME_SUMMARY_VARIANT },
            DEFAULT_HOME_SUMMARY_VARIANT
        ).distinct()

        return candidates.firstNotNullOfOrNull { candidate ->
            repository.findByCategoryAndCodeAndVariantAndActiveTrue(category, code, candidate)
                ?.toReviewedContent()
        }
    }

    private fun HomeSummaryInterpretationEntity.toReviewedContent(): ReviewedHomeSummaryInterpretation =
        ReviewedHomeSummaryInterpretation(
            dailyBody = dailyBody,
            personalBodyTemplate = personalBodyTemplate,
            points = parsePoints(pointsJson, category, code, variant)
        )

    private fun parsePoints(
        pointsJson: String,
        category: HomeSummaryInterpretationCategory,
        code: String,
        variant: String
    ): List<String>? =
        runCatching {
            objectMapper.readValue(pointsJson, POINTS_TYPE)
                .map(String::trim)
                .filter(String::isNotBlank)
                .takeIf { it.size == 3 }
        }.onFailure { ex ->
            logger.warn(
                "Failed to parse home summary reviewed points. category={}, code={}, variant={}",
                category,
                code,
                variant,
                ex
            )
        }.getOrNull()

    private companion object {
        private val logger = LoggerFactory.getLogger(HomeSummaryInterpretationService::class.java)
        private val POINTS_TYPE = object : TypeReference<List<String>>() {}
    }
}

const val DEFAULT_HOME_SUMMARY_VARIANT = "DEFAULT"
