package com.hwcompany.fortune_index.auth

import com.hwcompany.fortune_index.domain.model.BirthInfo
import com.hwcompany.fortune_index.domain.model.EmailVerificationPurpose
import com.hwcompany.fortune_index.domain.model.EmailVerificationToken
import com.hwcompany.fortune_index.domain.model.RefreshToken
import com.hwcompany.fortune_index.domain.model.RefreshTokenStatus
import com.hwcompany.fortune_index.domain.model.User
import com.hwcompany.fortune_index.domain.model.UserAccountStatus
import com.hwcompany.fortune_index.domain.model.SubscriptionTier
import com.hwcompany.fortune_index.history.UserRepository
import com.hwcompany.fortune_index.investment.VirtualInvestmentRepository
import com.hwcompany.fortune_index.saju.SajuPersistenceService
import com.hwcompany.fortune_index.tarot.DEFAULT_TAROT_DECK_VERSION_ID
import com.hwcompany.fortune_index.tarot.TarotDeckRole
import com.hwcompany.fortune_index.tarot.TarotDeckVersionRepository
import java.security.SecureRandom
import java.time.LocalDateTime
import java.time.ZoneOffset
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.ObjectProvider
import org.springframework.http.HttpStatus
import org.springframework.mail.SimpleMailMessage
import org.springframework.mail.javamail.JavaMailSender
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.server.ResponseStatusException

@Service
class AuthService(
    private val userRepository: UserRepository,
    private val virtualInvestmentRepository: VirtualInvestmentRepository,
    private val sajuPersistenceService: SajuPersistenceService,
    private val refreshTokenRepository: RefreshTokenRepository,
    private val emailVerificationTokenRepository: EmailVerificationTokenRepository,
    private val passwordEncoder: PasswordEncoder,
    private val jwtTokenService: JwtTokenService,
    private val authProperties: AuthProperties,
    private val tarotDeckVersionRepository: TarotDeckVersionRepository,
    mailSenderProvider: ObjectProvider<JavaMailSender>
) {
    private val mailSender = mailSenderProvider.getIfAvailable()

    @Transactional
    fun requestSignupCode(request: EmailCodeRequest): EmailCodeResponse {
        val email = normalizeEmail(request.email)
        if (userRepository.existsByEmail(email)) {
            throw ResponseStatusException(HttpStatus.CONFLICT, "email already exists: $email")
        }
        return issueEmailCode(email, EmailVerificationPurpose.SIGNUP)
    }

    @Transactional
    fun verifySignupCode(request: EmailCodeVerifyRequest): EmailVerificationResponse =
        verifyEmailCode(
            email = normalizeEmail(request.email),
            code = request.verificationCode,
            purpose = EmailVerificationPurpose.SIGNUP
        )

    @Transactional
    fun signUp(request: SignUpRequest): AuthResponse {
        val email = normalizeEmail(request.email)
        if (userRepository.existsByEmail(email)) {
            throw ResponseStatusException(HttpStatus.CONFLICT, "email already exists: $email")
        }

        val verification = requireVerifiedCode(email, request.verificationCode, EmailVerificationPurpose.SIGNUP)
        verification.verified = true
        verification.verifiedAt = verification.verifiedAt ?: LocalDateTime.now()

        val user = userRepository.save(
            User(
                name = request.name.trim(),
                email = email,
                passwordHash = passwordEncoder.encode(request.password),
                birthInfo = BirthInfo(
                    birthDate = request.birthDate,
                    birthTime = request.birthTime
                ),
                accountStatus = UserAccountStatus.ACTIVE,
                emailVerified = true,
                gender = request.gender,
                preferredTarotDeckId = DEFAULT_TAROT_DECK_VERSION_ID,
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
    fun requestPasswordResetCode(request: EmailCodeRequest): EmailCodeResponse {
        val email = normalizeEmail(request.email)
        val user = userRepository.findByEmail(email)
            ?: throw ResponseStatusException(HttpStatus.NOT_FOUND, "user not found for email: $email")
        ensureActiveUser(user)
        return issueEmailCode(email, EmailVerificationPurpose.PASSWORD_RESET)
    }

    @Transactional
    fun verifyPasswordResetCode(request: EmailCodeVerifyRequest): EmailVerificationResponse =
        verifyEmailCode(
            email = normalizeEmail(request.email),
            code = request.verificationCode,
            purpose = EmailVerificationPurpose.PASSWORD_RESET
        )

    @Transactional
    fun confirmPasswordReset(request: PasswordResetConfirmRequest) {
        val email = normalizeEmail(request.email)
        val user = userRepository.findByEmail(email)
            ?: throw ResponseStatusException(HttpStatus.NOT_FOUND, "user not found for email: $email")
        ensureActiveUser(user)

        requireVerifiedCode(email, request.verificationCode, EmailVerificationPurpose.PASSWORD_RESET)
        user.passwordHash = passwordEncoder.encode(request.newPassword)
        revokeAllRefreshTokens(user)
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

        val birthDate = request.birthDate
            ?: throw ResponseStatusException(HttpStatus.BAD_REQUEST, "birthDate is required")

        val birthInfoChanged = user.birthInfo.birthDate != birthDate || user.birthInfo.birthTime != request.birthTime
        val genderChanged = user.gender != request.gender

        user.name = request.name.trim()
        user.birthInfo.birthDate = birthDate
        user.birthInfo.birthTime = request.birthTime
        user.gender = request.gender
        user.preferredTarotDeckId = resolvePreferredTarotDeckId(
            requestedDeckVersionId = request.preferredTarotDeckId,
            fallbackDeckVersionId = user.preferredTarotDeckId,
            subscriptionTier = user.subscriptionTier
        )

        if (birthInfoChanged || genderChanged) {
            sajuPersistenceService.refreshResult(user)
        }

        return user.toCurrentUserResponse(
            virtualInvestmentEnabled = virtualInvestmentRepository.existsByUserId(requireNotNull(user.id))
        )
    }

    @Transactional(readOnly = true)
    fun getCurrentUser(authenticatedUser: AuthenticatedUser): CurrentUserResponse {
        val user = userRepository.findById(authenticatedUser.userId)
            .orElseThrow { ResponseStatusException(HttpStatus.NOT_FOUND, "user not found: ${authenticatedUser.userId}") }
        ensureActiveUser(user)
        return user.toCurrentUserResponse(
            virtualInvestmentEnabled = virtualInvestmentRepository.existsByUserId(requireNotNull(user.id))
        )
    }

    private fun issueEmailCode(email: String, purpose: EmailVerificationPurpose): EmailCodeResponse {
        emailVerificationTokenRepository.deleteAllByEmailAndPurposeAndExpiresAtBefore(
            email = email,
            purpose = purpose,
            expiresAt = LocalDateTime.now()
        )

        val code = generateVerificationCode()
        val expiresAt = LocalDateTime.now().plusMinutes(authProperties.email.codeValidityMinutes)
        val token = emailVerificationTokenRepository.save(
            EmailVerificationToken(
                email = email,
                purpose = purpose,
                verificationCode = code,
                expiresAt = expiresAt
            )
        )

        sendEmail(
            email = email,
            subject = when (purpose) {
                EmailVerificationPurpose.SIGNUP -> "[fortune_index] 회원가입 이메일 인증 코드"
                EmailVerificationPurpose.PASSWORD_RESET -> "[fortune_index] 비밀번호 재설정 인증 코드"
            },
            body = "인증 코드는 ${token.verificationCode} 입니다. ${authProperties.email.codeValidityMinutes}분 내에 입력해 주세요."
        )

        return EmailCodeResponse(
            email = email,
            purpose = purpose.name,
            expiresAt = expiresAt
        )
    }

    private fun verifyEmailCode(
        email: String,
        code: String,
        purpose: EmailVerificationPurpose
    ): EmailVerificationResponse {
        val token = requireVerifiedCode(email, code, purpose)
        val verifiedAt = token.verifiedAt ?: LocalDateTime.now()
        token.verified = true
        token.verifiedAt = verifiedAt

        return EmailVerificationResponse(
            email = email,
            purpose = purpose.name,
            verified = true,
            verifiedAt = verifiedAt
        )
    }

    private fun requireVerifiedCode(
        email: String,
        code: String,
        purpose: EmailVerificationPurpose
    ): EmailVerificationToken {
        val token = emailVerificationTokenRepository
            .findFirstByEmailAndPurposeAndVerificationCodeOrderByCreatedAtDesc(email, purpose, code)
            ?: throw ResponseStatusException(HttpStatus.BAD_REQUEST, "verification code not found")

        if (token.expiresAt.isBefore(LocalDateTime.now())) {
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "verification code expired")
        }

        return token
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

    private fun sendEmail(email: String, subject: String, body: String) {
        val sender = mailSender
        if (sender == null) {
            logger.warn("JavaMailSender not configured. email={}, subject={}, body={}", email, subject, body)
            return
        }

        runCatching {
            sender.send(
                SimpleMailMessage().apply {
                    from = authProperties.email.fromAddress
                    setTo(email)
                    this.subject = subject
                    text = body
                }
            )
        }.onFailure { ex ->
            logger.warn("Failed to send email to {}", email, ex)
        }
    }

    private fun normalizeEmail(email: String): String = email.trim().lowercase()

    private fun resolvePreferredTarotDeckId(
        requestedDeckVersionId: String?,
        fallbackDeckVersionId: String?,
        subscriptionTier: SubscriptionTier
    ): String {
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

    private fun generateVerificationCode(): String =
        (100000 + secureRandom.nextInt(900000)).toString()

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

    private fun User.toCurrentUserResponse(virtualInvestmentEnabled: Boolean): CurrentUserResponse =
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
            gender = gender,
            profileImageUrl = profileImageUrl,
            notificationEnabled = notificationEnabled,
            virtualInvestmentEnabled = virtualInvestmentEnabled,
            darkModeEnabled = darkModeEnabled,
            createdAt = createdAt.atOffset(ZoneOffset.UTC),
            lastLoginAt = lastLoginAt?.atOffset(ZoneOffset.UTC)
        )

    private companion object {
        private val logger = LoggerFactory.getLogger(AuthService::class.java)
        private val secureRandom = SecureRandom()
    }
}
