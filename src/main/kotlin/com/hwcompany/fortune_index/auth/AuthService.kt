package com.hwcompany.fortune_index.auth

import com.hwcompany.fortune_index.auth.email.EmailVerificationMailSender
import com.hwcompany.fortune_index.auth.email.EmailVerificationOutcome
import com.hwcompany.fortune_index.auth.email.EmailVerificationProperties
import com.hwcompany.fortune_index.auth.email.EmailVerificationResult
import com.hwcompany.fortune_index.auth.email.EmailVerificationStatus
import com.hwcompany.fortune_index.domain.model.BirthInfo
import com.hwcompany.fortune_index.domain.model.EmailVerificationPurpose
import com.hwcompany.fortune_index.domain.model.EmailVerificationToken
import com.hwcompany.fortune_index.domain.model.RefreshToken
import com.hwcompany.fortune_index.domain.model.RefreshTokenStatus
import com.hwcompany.fortune_index.domain.model.User
import com.hwcompany.fortune_index.domain.model.UserAccountStatus
import com.hwcompany.fortune_index.domain.model.SubscriptionTier
import com.hwcompany.fortune_index.domain.model.WesternZodiacSign
import com.hwcompany.fortune_index.history.UserRepository
import com.hwcompany.fortune_index.saju.SajuPersistenceService
import com.hwcompany.fortune_index.tarot.DEFAULT_TAROT_DECK_VERSION_ID
import com.hwcompany.fortune_index.tarot.TarotDeckRole
import com.hwcompany.fortune_index.tarot.TarotDeckVersionRepository
import com.hwcompany.fortune_index.tarot.resolveBirthTarotCard
import java.security.SecureRandom
import java.time.LocalDateTime
import java.time.ZoneOffset
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.server.ResponseStatusException

@Service
class AuthService(
    private val userRepository: UserRepository,
    private val sajuPersistenceService: SajuPersistenceService,
    private val refreshTokenRepository: RefreshTokenRepository,
    private val emailVerificationTokenRepository: EmailVerificationTokenRepository,
    private val passwordEncoder: PasswordEncoder,
    private val jwtTokenService: JwtTokenService,
    private val authProperties: AuthProperties,
    private val tarotDeckVersionRepository: TarotDeckVersionRepository,
    private val emailVerificationMailSender: EmailVerificationMailSender,
    private val emailVerificationProperties: EmailVerificationProperties
) {

    @Transactional
    fun requestSignupEmailVerification(request: EmailVerificationLinkRequest): EmailVerificationLinkResponse {
        val email = normalizeEmail(request.email)
        if (userRepository.existsByEmail(email)) {
            throw ResponseStatusException(HttpStatus.CONFLICT, "email already exists: $email")
        }
        return requestEmailVerificationLink(email, EmailVerificationPurpose.SIGNUP)
    }

    @Transactional
    fun signUp(request: SignUpRequest): AuthResponse {
        val email = consumeVerifiedSignupToken(request.emailVerificationToken)
        if (userRepository.existsByEmail(email)) {
            throw ResponseStatusException(HttpStatus.CONFLICT, "email already exists: $email")
        }
        requireLatitude(request.birthLatitude)
        requireLongitude(request.birthLongitude)

        val user = userRepository.save(
            User(
                name = request.name.trim(),
                email = email,
                passwordHash = passwordEncoder.encode(request.password),
                birthInfo = BirthInfo(
                    birthDate = request.birthDate,
                    birthTime = request.birthTime,
                    birthPlaceName = request.birthPlaceName.trim(),
                    birthLatitude = request.birthLatitude,
                    birthLongitude = request.birthLongitude
                ),
                accountStatus = UserAccountStatus.ACTIVE,
                emailVerified = true,
                gender = request.gender,
                westernZodiac = WesternZodiacSign.from(request.birthDate),
                preferredTarotDeckId = DEFAULT_TAROT_DECK_VERSION_ID,
                birthTarotCardCode = resolveBirthTarotCard(request.birthDate.toString()).code,
                investmentRiskProfile = request.investmentRiskProfile,
                preferredSectors = request.preferredSectors.toMutableSet()
            )
        )
        sajuPersistenceService.saveInitialResult(user)

        return buildAuthResponse(user)
    }

    @Transactional
    fun login(request: LoginRequest): AuthResponse {
        val email = normalizeEmail(request.email)
        val user = userRepository.findByEmail(email)
            ?: throw ResponseStatusException(HttpStatus.UNAUTHORIZED, "invalid email or password")

        ensureActiveUser(user)
        if (!passwordEncoder.matches(request.password, user.passwordHash)) {
            throw ResponseStatusException(HttpStatus.UNAUTHORIZED, "invalid email or password")
        }

        user.lastLoginAt = LocalDateTime.now()
        return buildAuthResponse(user)
    }

    @Transactional
    fun refresh(request: TokenRefreshRequest): AuthResponse {
        val parsed = runCatching { jwtTokenService.parse(request.refreshToken) }
            .getOrElse { throw ResponseStatusException(HttpStatus.UNAUTHORIZED, "invalid refresh token") }

        if (parsed.tokenType != JwtTokenService.TOKEN_TYPE_REFRESH) {
            throw ResponseStatusException(HttpStatus.UNAUTHORIZED, "invalid refresh token type")
        }

        val savedToken = refreshTokenRepository.findByTokenValue(request.refreshToken)
            ?: throw ResponseStatusException(HttpStatus.UNAUTHORIZED, "refresh token not found")

        if (savedToken.status != RefreshTokenStatus.ACTIVE || savedToken.expiresAt.isBefore(LocalDateTime.now())) {
            savedToken.status = if (savedToken.expiresAt.isBefore(LocalDateTime.now())) {
                RefreshTokenStatus.EXPIRED
            } else {
                RefreshTokenStatus.REVOKED
            }
            throw ResponseStatusException(HttpStatus.UNAUTHORIZED, "refresh token is not active")
        }

        val user = savedToken.user
        ensureActiveUser(user)

        savedToken.status = RefreshTokenStatus.REVOKED
        savedToken.revokedAt = LocalDateTime.now()

        return buildAuthResponse(user)
    }

    @Transactional
    fun logout(request: LogoutRequest) {
        val refreshToken = refreshTokenRepository.findByTokenValue(request.refreshToken)
            ?: return

        refreshToken.status = RefreshTokenStatus.REVOKED
        refreshToken.revokedAt = LocalDateTime.now()
    }

    @Transactional
    fun requestPasswordResetEmailVerification(request: EmailVerificationLinkRequest): EmailVerificationLinkResponse {
        val email = normalizeEmail(request.email)
        val user = userRepository.findByEmail(email)
            ?: throw ResponseStatusException(HttpStatus.NOT_FOUND, "user not found for email: $email")
        ensureActiveUser(user)
        return requestEmailVerificationLink(email, EmailVerificationPurpose.PASSWORD_RESET)
    }

    @Transactional
    fun confirmPasswordReset(request: PasswordResetConfirmRequest) {
        val token = emailVerificationTokenRepository
            .findFirstByPurposeAndVerificationCodeOrderByCreatedAtDesc(
                EmailVerificationPurpose.PASSWORD_RESET,
                request.resetToken
            )
            ?: throw ResponseStatusException(HttpStatus.BAD_REQUEST, "password reset token not found")

        if (!token.verified) {
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "password reset email verification required")
        }

        if (token.expiresAt.isBefore(LocalDateTime.now())) {
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "password reset token expired")
        }

        val email = token.email
        val user = userRepository.findByEmail(email)
            ?: throw ResponseStatusException(HttpStatus.NOT_FOUND, "user not found for email: $email")
        ensureActiveUser(user)

        user.passwordHash = passwordEncoder.encode(request.newPassword)
        emailVerificationTokenRepository.delete(token)
        revokeAllRefreshTokens(user)
    }

    @Transactional
    fun verifyEmailToken(tokenValue: String): EmailVerificationOutcome? {
        val token = emailVerificationTokenRepository
            .findFirstByVerificationCodeOrderByCreatedAtDesc(tokenValue)
            ?: return null

        val now = LocalDateTime.now()
        if (token.expiresAt.isBefore(now)) {
            return emailVerificationOutcome(EmailVerificationResult.EXPIRED, token)
        }

        if (!token.verified) {
            token.verified = true
            token.verifiedAt = now
        }

        return emailVerificationOutcome(EmailVerificationResult.SUCCESS, token)
    }

    @Transactional
    fun withdraw(authenticatedUser: AuthenticatedUser, request: WithdrawRequest) {
        val user = userRepository.findById(authenticatedUser.userId)
            .orElseThrow { ResponseStatusException(HttpStatus.NOT_FOUND, "user not found: ${authenticatedUser.userId}") }
        ensureActiveUser(user)

        if (!passwordEncoder.matches(request.password, user.passwordHash)) {
            throw ResponseStatusException(HttpStatus.UNAUTHORIZED, "invalid password")
        }

        val now = LocalDateTime.now()
        user.accountStatus = UserAccountStatus.WITHDRAWN
        user.withdrawnAt = now
        user.emailVerified = false
        user.email = "withdrawn-${requireNotNull(user.id)}-$now-${user.email}".take(120)
        user.passwordHash = passwordEncoder.encode("withdrawn-${requireNotNull(user.id)}")
        user.preferredSectors.clear()

        revokeAllRefreshTokens(user)
    }

    @Transactional
    fun updateCurrentUser(authenticatedUser: AuthenticatedUser, request: UpdateCurrentUserRequest): CurrentUserResponse {
        val user = userRepository.findById(authenticatedUser.userId)
            .orElseThrow { ResponseStatusException(HttpStatus.NOT_FOUND, "user not found: ${authenticatedUser.userId}") }
        ensureActiveUser(user)
        validateBirthLocationUpdate(request)

        val updatedName = request.name?.trim()
        if (updatedName != null && updatedName.isBlank()) {
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "name must not be blank")
        }

        val birthDate = request.birthDate ?: user.birthInfo.birthDate
        val birthTime = if (request.birthDate != null || request.birthTime != null) {
            request.birthTime
        } else {
            user.birthInfo.birthTime
        }
        val birthPlaceName = request.birthPlaceName?.trim()?.takeIf { it.isNotBlank() } ?: user.birthInfo.birthPlaceName
        val birthLatitude = request.birthLatitude ?: user.birthInfo.birthLatitude
        val birthLongitude = request.birthLongitude ?: user.birthInfo.birthLongitude
        val gender = request.gender ?: user.gender

        val birthInfoChanged = user.birthInfo.birthDate != birthDate || user.birthInfo.birthTime != birthTime
        val genderChanged = user.gender != gender

        updatedName?.let { user.name = it }
        user.birthInfo.birthDate = birthDate
        user.birthInfo.birthTime = birthTime
        user.birthInfo.birthPlaceName = birthPlaceName
        user.birthInfo.birthLatitude = birthLatitude
        user.birthInfo.birthLongitude = birthLongitude
        user.gender = gender
        if (request.birthDate != null) {
            user.westernZodiac = WesternZodiacSign.from(birthDate)
            user.birthTarotCardCode = resolveBirthTarotCard(birthDate.toString()).code
        }
        request.preferredTarotDeckId?.let {
            user.preferredTarotDeckId = resolvePreferredTarotDeckId(
                requestedDeckVersionId = it,
                fallbackDeckVersionId = user.preferredTarotDeckId,
                subscriptionTier = user.subscriptionTier
            )
        }
        request.investmentRiskProfile?.let { user.investmentRiskProfile = it }
        request.preferredSectors?.let {
            user.preferredSectors.clear()
            user.preferredSectors.addAll(it)
        }
        request.notificationEnabled?.let { user.notificationEnabled = it }
        request.darkModeEnabled?.let { user.darkModeEnabled = it }

        if (birthInfoChanged || genderChanged) {
            sajuPersistenceService.refreshResult(user)
        }

        return user.toCurrentUserResponse()
    }

    @Transactional(readOnly = true)
    fun getCurrentUser(authenticatedUser: AuthenticatedUser): CurrentUserResponse {
        val user = userRepository.findById(authenticatedUser.userId)
            .orElseThrow { ResponseStatusException(HttpStatus.NOT_FOUND, "user not found: ${authenticatedUser.userId}") }
        ensureActiveUser(user)
        return user.toCurrentUserResponse()
    }

    private fun requestEmailVerificationLink(
        email: String,
        purpose: EmailVerificationPurpose
    ): EmailVerificationLinkResponse {
        emailVerificationTokenRepository.deleteAllByEmailAndPurposeAndExpiresAtBefore(
            email = email,
            purpose = purpose,
            expiresAt = LocalDateTime.now()
        )

        val verificationToken = generateVerificationToken()
        val expiresAt = LocalDateTime.now().plusMinutes(emailVerificationLinkValidityMinutes())
        val token = emailVerificationTokenRepository.save(
            EmailVerificationToken(
                email = email,
                purpose = purpose,
                verificationCode = verificationToken,
                expiresAt = expiresAt
            )
        )

        sendEmailVerificationLink(email, token.verificationCode, purpose)

        return EmailVerificationLinkResponse(
            email = email,
            purpose = purpose.name,
            expiresAt = expiresAt
        )
    }

    private fun consumeVerifiedSignupToken(tokenValue: String): String {
        val token = emailVerificationTokenRepository
            .findFirstByPurposeAndVerificationCodeOrderByCreatedAtDesc(
                EmailVerificationPurpose.SIGNUP,
                tokenValue
            )
            ?: throw ResponseStatusException(HttpStatus.BAD_REQUEST, "email verification token not found")

        if (!token.verified) {
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "email verification required")
        }

        if (token.expiresAt.isBefore(LocalDateTime.now())) {
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "email verification token expired")
        }

        val email = token.email
        emailVerificationTokenRepository.delete(token)
        return email
    }

    private fun buildAuthResponse(user: User): AuthResponse {
        val accessToken = jwtTokenService.generateAccessToken(requireNotNull(user.id), user.email)
        val refreshToken = jwtTokenService.generateRefreshToken(requireNotNull(user.id), user.email)

        refreshTokenRepository.save(
            RefreshToken(
                user = user,
                tokenValue = refreshToken.token,
                expiresAt = refreshToken.expiresAt
            )
        )

        return AuthResponse(
            user = user.toResponse(),
            tokens = AuthTokenResponse(
                accessToken = accessToken.token,
                refreshToken = refreshToken.token,
                accessTokenExpiresAt = accessToken.expiresAt,
                refreshTokenExpiresAt = refreshToken.expiresAt
            )
        )
    }

    private fun revokeAllRefreshTokens(user: User) {
        refreshTokenRepository.findAllByUserIdAndStatus(requireNotNull(user.id), RefreshTokenStatus.ACTIVE)
            .forEach {
                it.status = RefreshTokenStatus.REVOKED
                it.revokedAt = LocalDateTime.now()
            }
    }

    private fun ensureActiveUser(user: User) {
        if (user.accountStatus != UserAccountStatus.ACTIVE) {
            throw ResponseStatusException(HttpStatus.FORBIDDEN, "user account is not active")
        }
    }

    @Transactional(readOnly = true)
    fun getSignupEmailVerificationStatus(email: String): EmailVerificationStatus {
        val normalizedEmail = normalizeEmail(email)
        val latestVerification = emailVerificationTokenRepository
            .findFirstByEmailAndPurposeOrderByCreatedAtDesc(normalizedEmail, EmailVerificationPurpose.SIGNUP)
            ?: throw ResponseStatusException(HttpStatus.NOT_FOUND, "email verification not found: $normalizedEmail")

        if (latestVerification.verified && latestVerification.expiresAt.isAfter(LocalDateTime.now())) {
            return EmailVerificationStatus.VERIFIED
        }

        return EmailVerificationStatus.PENDING
    }

    private fun sendEmailVerificationLink(
        email: String,
        verificationToken: String,
        purpose: EmailVerificationPurpose
    ) {
        emailVerificationMailSender.send(
            email = email,
            verificationUrl = emailVerificationProperties.verificationUrl(verificationToken),
            purpose = purpose
        )
    }

    private fun emailVerificationOutcome(
        result: EmailVerificationResult,
        token: EmailVerificationToken
    ): EmailVerificationOutcome =
        EmailVerificationOutcome(
            result = result,
            email = token.email,
            purpose = token.purpose,
            token = token.verificationCode
        )

    private fun emailVerificationLinkValidityMinutes(): Long =
        emailVerificationProperties.expirationMinutes

    private fun normalizeEmail(email: String): String = email.trim().lowercase()

    private fun validateBirthLocationUpdate(request: UpdateCurrentUserRequest) {
        if (request.birthPlaceName != null && request.birthPlaceName.isBlank()) {
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "birthPlaceName must not be blank")
        }
        val providedFields = listOf(
            request.birthPlaceName != null,
            request.birthLatitude != null,
            request.birthLongitude != null
        ).count { it }
        if (providedFields != 0 && providedFields != 3) {
            throw ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "birthPlaceName, birthLatitude, birthLongitude must be provided together"
            )
        }
        request.birthLatitude?.let {
            requireLatitude(it)
        }
        request.birthLongitude?.let {
            requireLongitude(it)
        }
    }

    private fun requireLatitude(value: Double) {
        if (value !in -90.0..90.0) {
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "birthLatitude must be between -90 and 90")
        }
    }

    private fun requireLongitude(value: Double) {
        if (value !in -180.0..180.0) {
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "birthLongitude must be between -180 and 180")
        }
    }

    private fun resolvePreferredTarotDeckId(
        requestedDeckVersionId: String?,
        fallbackDeckVersionId: String?,
        subscriptionTier: SubscriptionTier
    ): String {
        tarotDeckVersionRepository.findFirstByActiveTrueAndDeckRoleOrderByDisplayOrderAscNameAsc(TarotDeckRole.MAIN)
            ?.takeIf { subscriptionTier.ordinal >= it.requiredSubscriptionTier.ordinal }
            ?.let { return it.id }

        val candidateId = requestedDeckVersionId?.trim()?.ifBlank { null }
            ?: fallbackDeckVersionId?.trim()?.ifBlank { null }
            ?: DEFAULT_TAROT_DECK_VERSION_ID
        val deck = tarotDeckVersionRepository.findById(candidateId).orElseThrow {
            ResponseStatusException(HttpStatus.BAD_REQUEST, "invalid preferredTarotDeckId: $candidateId")
        }
        if (!deck.active || deck.deckRole != TarotDeckRole.MAIN) {
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "invalid preferredTarotDeckId: $candidateId")
        }
        if (subscriptionTier.ordinal < deck.requiredSubscriptionTier.ordinal) {
            return DEFAULT_TAROT_DECK_VERSION_ID
        }
        return deck.id
    }

    private fun generateVerificationToken(): String =
        (1..EMAIL_VERIFICATION_TOKEN_LENGTH)
            .map { EMAIL_VERIFICATION_TOKEN_ALPHABET[secureRandom.nextInt(EMAIL_VERIFICATION_TOKEN_ALPHABET.length)] }
            .joinToString("")

    private fun User.toResponse(): AuthUserResponse =
        AuthUserResponse(
            id = requireNotNull(id),
            name = name,
            email = email,
            emailVerified = emailVerified,
            subscriptionTier = subscriptionTier,
            preferredTarotDeckId = preferredTarotDeckId,
            investmentRiskProfile = investmentRiskProfile,
            preferredSectors = preferredSectors.sortedBy { it.name }
        )

    private fun User.toCurrentUserResponse(): CurrentUserResponse =
        CurrentUserResponse(
            id = requireNotNull(id),
            name = name,
            email = email,
            emailVerified = emailVerified,
            subscriptionTier = subscriptionTier,
            preferredTarotDeckId = preferredTarotDeckId,
            investmentRiskProfile = investmentRiskProfile,
            preferredSectors = preferredSectors.sortedBy { it.name },
            birthDate = birthInfo.birthDate,
            birthTime = birthInfo.birthTime,
            birthPlaceName = birthInfo.birthPlaceName,
            birthLatitude = birthInfo.birthLatitude,
            birthLongitude = birthInfo.birthLongitude,
            gender = gender,
            profileImageUrl = profileImageUrl,
            notificationEnabled = notificationEnabled,
            darkModeEnabled = darkModeEnabled,
            createdAt = createdAt.atOffset(ZoneOffset.UTC),
            lastLoginAt = lastLoginAt?.atOffset(ZoneOffset.UTC)
        )

    private companion object {
        private val logger = LoggerFactory.getLogger(AuthService::class.java)
        private val secureRandom = SecureRandom()
        private const val EMAIL_VERIFICATION_TOKEN_LENGTH = 20
        private const val EMAIL_VERIFICATION_TOKEN_ALPHABET =
            "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789"
    }
}
