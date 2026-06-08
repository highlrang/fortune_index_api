package com.hwcompany.fortune_index.domain.model

import com.hwcompany.fortune_index.common.SeoulTime
import jakarta.persistence.CollectionTable
import jakarta.persistence.Column
import jakarta.persistence.Embeddable
import jakarta.persistence.ElementCollection
import jakarta.persistence.Embedded
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import java.time.LocalDateTime

@Entity
@Table(name = "saju_results")
data class SajuResult(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    var user: User,

    @ElementCollection
    @CollectionTable(
        name = "saju_result_heavenly_stems",
        joinColumns = [JoinColumn(name = "saju_result_id")]
    )
    var heavenlyStems: MutableList<SajuStemRecord> = mutableListOf(),

    @ElementCollection
    @CollectionTable(
        name = "saju_result_earthly_branches",
        joinColumns = [JoinColumn(name = "saju_result_id")]
    )
    var earthlyBranches: MutableList<SajuBranchRecord> = mutableListOf(),

    @Embedded
    var fiveElements: FiveElementsProfile,

    @Column(nullable = false)
    var analyzedAt: LocalDateTime = SeoulTime.now()
)

@Embeddable
data class SajuStemRecord(
    @Column(name = "pillar_order", nullable = false)
    var pillarOrder: Int = 0,

    @Column(name = "code", nullable = false, length = 10)
    var code: String = "",

    @Column(name = "label_ko", nullable = false, length = 20)
    var labelKo: String = "",

    @Column(name = "sort_order", nullable = false)
    var sortOrder: Int = 0
)

@Embeddable
data class SajuBranchRecord(
    @Column(name = "pillar_order", nullable = false)
    var pillarOrder: Int = 0,

    @Column(name = "code", nullable = false, length = 10)
    var code: String = "",

    @Column(name = "label_ko", nullable = false, length = 20)
    var labelKo: String = "",

    @Column(name = "sort_order", nullable = false)
    var sortOrder: Int = 0
)
