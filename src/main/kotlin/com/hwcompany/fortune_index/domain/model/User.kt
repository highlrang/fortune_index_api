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
import java.time.LocalDateTime

@Entity
@Table(name = "users")
data class User(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,

    @Column(nullable = false, length = 50)
    var name: String,

    @Column(nullable = false, unique = true, length = 120)
    var email: String,

    @Column(name = "password_hash", nullable = false, length = 255)
    var passwordHash: String,

    @Embedded
    var birthInfo: BirthInfo,

    @Enumerated(EnumType.STRING)
    @Column(name = "account_status", nullable = false, length = 20)
    var accountStatus: UserAccountStatus = UserAccountStatus.ACTIVE,

    @Column(name = "email_verified", nullable = false)
    var emailVerified: Boolean = false,

    @Column(name = "last_login_at")
    var lastLoginAt: LocalDateTime? = null,

    @Column(name = "withdrawn_at")
    var withdrawnAt: LocalDateTime? = null,

    @Enumerated(EnumType.STRING)
    @Column(name = "investment_risk_profile", nullable = false, length = 20)
    var investmentRiskProfile: InvestmentRiskProfile = InvestmentRiskProfile.STABLE,

    @ElementCollection
    @CollectionTable(
        name = "user_preferred_sectors",
        joinColumns = [JoinColumn(name = "user_id")]
    )
    @Column(name = "sector", nullable = false, length = 30)
    @Enumerated(EnumType.STRING)
    var preferredSectors: MutableSet<InvestmentSector> = mutableSetOf()
)
