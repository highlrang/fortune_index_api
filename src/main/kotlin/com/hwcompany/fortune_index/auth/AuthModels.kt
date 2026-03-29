package com.hwcompany.fortune_index.auth

import com.hwcompany.fortune_index.domain.model.InvestmentRiskProfile
import com.hwcompany.fortune_index.domain.model.InvestmentSector
import com.hwcompany.fortune_index.domain.model.SubscriptionTier
import com.hwcompany.fortune_index.domain.model.UserGender
import com.fasterxml.jackson.annotation.JsonFormat
import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotEmpty
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Size
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.OffsetDateTime

data class SignUpRequest(
    @field:NotBlank
    @field:Size(max = 50)
    val name: String,
    @field:Email
    @field:NotBlank
    val email: String,
    @field:NotBlank
    @field:Size(min = 8, max = 100)
    val password: String,
    @field:NotBlank
    @field:Size(min = 4, max = 20)
    val verificationCode: String,
    @field:NotNull
    val birthDate: LocalDate,
    @field:JsonFormat(pattern = "HH:mm")
    val birthTime: LocalTime? = null,
    val gender: UserGender = UserGender.M,
    val investmentRiskProfile: InvestmentRiskProfile = InvestmentRiskProfile.STABLE,
    val preferredSectors: Set<InvestmentSector> = emptySet()
)

data class LoginRequest(
    @field:Email
    @field:NotBlank
    val email: String,
    @field:NotBlank
    val password: String
)

data class TokenRefreshRequest(
    @field:NotBlank
    val refreshToken: String
)

data class LogoutRequest(
    @field:NotBlank
    val refreshToken: String
)

data class WithdrawRequest(
    @field:NotBlank
    val password: String
)

data class UpdateCurrentUserRequest(
    @field:Size(max = 50)
    val name: String? = null,
    val birthDate: LocalDate? = null,
    @field:JsonFormat(pattern = "HH:mm")
    val birthTime: LocalTime? = null,
    val gender: UserGender? = null,
    val preferredTarotDeckId: String? = null,
    val notificationEnabled: Boolean? = null,
    val darkModeEnabled: Boolean? = null
)

data class EmailCodeRequest(
    @field:Email
    @field:NotBlank
    val email: String
)

data class EmailCodeVerifyRequest(
    @field:Email
    @field:NotBlank
    val email: String,
    @field:NotBlank
    @field:Size(min = 4, max = 20)
    val verificationCode: String
)

data class PasswordResetConfirmRequest(
    @field:Email
    @field:NotBlank
    val email: String,
    @field:NotBlank
    @field:Size(min = 4, max = 20)
    val verificationCode: String,
    @field:NotBlank
    @field:Size(min = 8, max = 100)
    val newPassword: String
)

data class AuthTokenResponse(
    val accessToken: String,
    val refreshToken: String,
    val tokenType: String = "Bearer",
    val accessTokenExpiresAt: LocalDateTime,
    val refreshTokenExpiresAt: LocalDateTime
)

data class AuthUserResponse(
    val id: Long,
    val name: String,
    val email: String,
    val emailVerified: Boolean,
    val subscriptionTier: SubscriptionTier,
    val preferredTarotDeckId: String?,
    val investmentRiskProfile: InvestmentRiskProfile,
    val preferredSectors: List<InvestmentSector>
)

data class CurrentUserResponse(
    val id: Long,
    val name: String,
    val email: String,
    val emailVerified: Boolean,
    val subscriptionTier: SubscriptionTier,
    val preferredTarotDeckId: String?,
    val investmentRiskProfile: InvestmentRiskProfile,
    val preferredSectors: List<InvestmentSector>,
    val birthDate: LocalDate,
    val birthTime: LocalTime?,
    val gender: UserGender,
    val profileImageUrl: String?,
    val notificationEnabled: Boolean,
    val virtualInvestmentEnabled: Boolean,
    val darkModeEnabled: Boolean,
    val createdAt: OffsetDateTime,
    val lastLoginAt: OffsetDateTime?
)

data class AuthResponse(
    val user: AuthUserResponse,
    val tokens: AuthTokenResponse
)

data class EmailCodeResponse(
    val email: String,
    val purpose: String,
    val expiresAt: LocalDateTime
)

data class EmailVerificationResponse(
    val email: String,
    val purpose: String,
    val verified: Boolean,
    val verifiedAt: LocalDateTime
)

data class MessageResponse(
    val message: String
)

data class AuthenticatedUser(
    val userId: Long,
    val email: String
)
