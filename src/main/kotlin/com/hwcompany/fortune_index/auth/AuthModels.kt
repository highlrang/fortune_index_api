package com.hwcompany.fortune_index.auth

import com.hwcompany.fortune_index.domain.model.InvestmentRiskProfile
import com.hwcompany.fortune_index.domain.model.InvestmentSector
import com.hwcompany.fortune_index.domain.model.SubscriptionTier
import com.hwcompany.fortune_index.domain.model.UserGender
import com.fasterxml.jackson.annotation.JsonFormat
import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotBlank
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
    @field:NotBlank
    @field:Size(min = 8, max = 100)
    val password: String,
    @field:NotNull
    val birthDate: LocalDate,
    @field:JsonFormat(pattern = "HH:mm")
    val birthTime: LocalTime? = null,
    @field:NotBlank
    @field:Size(max = 100)
    val birthPlaceName: String,
    @field:NotNull
    val birthLatitude: Double,
    @field:NotNull
    val birthLongitude: Double,
    val gender: UserGender = UserGender.M,
    val investmentRiskProfile: InvestmentRiskProfile = InvestmentRiskProfile.STABLE,
    val preferredSectors: Set<InvestmentSector> = emptySet(),
    @field:NotBlank
    @field:Size(max = 100)
    val emailVerificationToken: String
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
    @field:Size(max = 100)
    val birthPlaceName: String? = null,
    val birthLatitude: Double? = null,
    val birthLongitude: Double? = null,
    val gender: UserGender? = null,
    val preferredTarotDeckId: String? = null,
    val investmentRiskProfile: InvestmentRiskProfile? = null,
    @field:Size(min = 1)
    val preferredSectors: Set<InvestmentSector>? = null,
    val notificationEnabled: Boolean? = null,
    val darkModeEnabled: Boolean? = null
)

data class EmailVerificationLinkRequest(
    @field:Email
    @field:NotBlank
    val email: String
)

data class PasswordResetConfirmRequest(
    @field:NotBlank
    val resetToken: String,
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
    val birthPlaceName: String?,
    val birthLatitude: Double?,
    val birthLongitude: Double?,
    val gender: UserGender,
    val profileImageUrl: String?,
    val notificationEnabled: Boolean,
    val darkModeEnabled: Boolean,
    val createdAt: OffsetDateTime,
    val lastLoginAt: OffsetDateTime?
)

data class AuthResponse(
    val user: AuthUserResponse,
    val tokens: AuthTokenResponse
)

data class EmailVerificationLinkResponse(
    val email: String,
    val purpose: String,
    val expiresAt: LocalDateTime
)

data class MessageResponse(
    val message: String
)

data class AuthenticatedUser(
    val userId: Long,
    val email: String
)
