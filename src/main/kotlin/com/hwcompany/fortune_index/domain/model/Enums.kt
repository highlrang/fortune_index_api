package com.hwcompany.fortune_index.domain.model

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

enum class UserAccountStatus {
    ACTIVE,
    WITHDRAWN
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

enum class TarotOrientation {
    UPRIGHT,
    REVERSED
}

enum class ConsultingFeedback {
    HELPFUL,
    NOT_HELPFUL
}
