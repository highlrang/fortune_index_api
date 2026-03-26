package com.hwcompany.fortune_index.domain.model

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonValue

enum class InvestmentSector {
    TECHNOLOGY,
    FINANCE,
    HEALTHCARE,
    ENERGY,
    CONSUMER,
    INDUSTRIAL,
    MATERIALS,
    TELECOMMUNICATION,
    REAL_ESTATE,
    ETF
}

enum class InvestmentRiskProfile {
    STABLE,
    AGGRESSIVE
}

enum class SubscriptionTier {
    FREE,
    PREMIUM
}

enum class UserAccountStatus {
    ACTIVE,
    WITHDRAWN
}

enum class UserGender {
    F,
    M;

    @JsonValue
    fun toJson(): String = name

    companion object {
        @JvmStatic
        @JsonCreator
        fun from(value: String): UserGender = fromNullable(value)
            ?: throw IllegalArgumentException("Unsupported UserGender value: $value")

        fun fromNullable(value: String?): UserGender? {
            val normalized = value?.trim()?.uppercase() ?: return null
            return when (normalized) {
                "F", "FEMALE", "WOMAN" -> F
                "M", "MALE", "MAN" -> M
                else -> null
            }
        }
    }
}

enum class EmailVerificationPurpose {
    SIGNUP,
    PASSWORD_RESET
}

enum class RefreshTokenStatus {
    ACTIVE,
    REVOKED,
    EXPIRED
}

enum class HeavenlyStem {
    GAP,
    EUL,
    BYEONG,
    JEONG,
    MU,
    GI,
    GYEONG,
    SIN,
    IM,
    GYE
}

fun HeavenlyStem.labelKo(): String =
    when (this) {
        HeavenlyStem.GAP -> "갑"
        HeavenlyStem.EUL -> "을"
        HeavenlyStem.BYEONG -> "병"
        HeavenlyStem.JEONG -> "정"
        HeavenlyStem.MU -> "무"
        HeavenlyStem.GI -> "기"
        HeavenlyStem.GYEONG -> "경"
        HeavenlyStem.SIN -> "신"
        HeavenlyStem.IM -> "임"
        HeavenlyStem.GYE -> "계"
    }

fun HeavenlyStem.sortOrder(): Int = ordinal + 1

enum class EarthlyBranch {
    JA,
    CHUK,
    IN,
    MYO,
    JIN,
    SA,
    O,
    MI,
    SIN,
    YU,
    SUL,
    HAE
}

fun EarthlyBranch.labelKo(): String =
    when (this) {
        EarthlyBranch.JA -> "자"
        EarthlyBranch.CHUK -> "축"
        EarthlyBranch.IN -> "인"
        EarthlyBranch.MYO -> "묘"
        EarthlyBranch.JIN -> "진"
        EarthlyBranch.SA -> "사"
        EarthlyBranch.O -> "오"
        EarthlyBranch.MI -> "미"
        EarthlyBranch.SIN -> "신"
        EarthlyBranch.YU -> "유"
        EarthlyBranch.SUL -> "술"
        EarthlyBranch.HAE -> "해"
    }

fun EarthlyBranch.sortOrder(): Int = ordinal + 1

enum class TarotOrientation {
    UPRIGHT,
    REVERSED
}

enum class ConsultingFeedback {
    HELPFUL,
    NOT_HELPFUL
}
