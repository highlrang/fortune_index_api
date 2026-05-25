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

    @Column(name = "created_at", nullable = false)
    var createdAt: LocalDateTime = LocalDateTime.now(),

    @Column(name = "withdrawn_at")
    var withdrawnAt: LocalDateTime? = null,

    @Enumerated(EnumType.STRING)
    @Column(name = "gender", nullable = false, length = 1)
    var gender: UserGender = UserGender.M,

    @Enumerated(EnumType.STRING)
    @Column(name = "western_zodiac", length = 20)
    var westernZodiac: WesternZodiacSign? = null,

    @Column(name = "profile_image_url", length = 500)
    var profileImageUrl: String? = null,

    @Column(name = "preferred_tarot_deck_id", length = 100)
    var preferredTarotDeckId: String? = null,

    @Column(name = "birth_tarot_card_code", length = 60)
    var birthTarotCardCode: String? = null,

    @Column(name = "notification_enabled", nullable = false)
    var notificationEnabled: Boolean = true,

    @Column(name = "dark_mode_enabled", nullable = false)
    var darkModeEnabled: Boolean = true,

    @Enumerated(EnumType.STRING)
    @Column(name = "investment_risk_profile", nullable = false, length = 20)
    var investmentRiskProfile: InvestmentRiskProfile = InvestmentRiskProfile.STABLE,

    @Enumerated(EnumType.STRING)
    @Column(name = "consulting_tone", nullable = false, length = 20)
    var consultingTone: ConsultingTone = ConsultingTone.FRIENDLY,

    @Enumerated(EnumType.STRING)
    @Column(name = "subscription_tier", nullable = false, length = 20)
    var subscriptionTier: SubscriptionTier = SubscriptionTier.FREE,

    @ElementCollection
    @CollectionTable(
        name = "user_preferred_sectors",
        joinColumns = [JoinColumn(name = "user_id")]
    )
    @Column(name = "sector", nullable = false, length = 30)
    @Enumerated(EnumType.STRING)
    var preferredSectors: MutableSet<InvestmentSector> = mutableSetOf()
)
