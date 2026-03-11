package com.hwcompany.fortune_index.domain.model

import jakarta.persistence.CollectionTable
import jakarta.persistence.Column
import jakarta.persistence.ElementCollection
import jakarta.persistence.Embedded
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.Table

@Entity
@Table(name = "users")
data class User(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,

    @Column(nullable = false, length = 50)
    var name: String,

    @Embedded
    var birthInfo: BirthInfo,

    @ElementCollection
    @CollectionTable(
        name = "user_preferred_sectors",
        joinColumns = [JoinColumn(name = "user_id")]
    )
    @Column(name = "sector", nullable = false, length = 30)
    @Enumerated(EnumType.STRING)
    var preferredSectors: MutableSet<InvestmentSector> = mutableSetOf()
)
