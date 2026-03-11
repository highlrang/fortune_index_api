package com.hwcompany.fortune_index.domain.model

import jakarta.persistence.CollectionTable
import jakarta.persistence.Column
import jakarta.persistence.ElementCollection
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
    @Column(name = "heavenly_stem", nullable = false, length = 10)
    @Enumerated(EnumType.STRING)
    var heavenlyStems: MutableList<HeavenlyStem> = mutableListOf(),

    @ElementCollection
    @CollectionTable(
        name = "saju_result_earthly_branches",
        joinColumns = [JoinColumn(name = "saju_result_id")]
    )
    @Column(name = "earthly_branch", nullable = false, length = 10)
    @Enumerated(EnumType.STRING)
    var earthlyBranches: MutableList<EarthlyBranch> = mutableListOf(),

    @Embedded
    var fiveElements: FiveElementsProfile,

    @Column(nullable = false)
    var analyzedAt: LocalDateTime = LocalDateTime.now()
)
