package com.hwcompany.fortune_index.auth

import com.hwcompany.fortune_index.auth.email.EmailVerificationRepository
import com.hwcompany.fortune_index.domain.model.BirthInfo
import com.hwcompany.fortune_index.domain.model.InvestmentRiskProfile
import com.hwcompany.fortune_index.domain.model.InvestmentSector
import com.hwcompany.fortune_index.domain.model.SubscriptionTier
import com.hwcompany.fortune_index.domain.model.User
import com.hwcompany.fortune_index.domain.model.UserAccountStatus
import com.hwcompany.fortune_index.domain.model.UserGender
import com.hwcompany.fortune_index.history.UserRepository
import com.hwcompany.fortune_index.investment.VirtualInvestmentRepository
import com.hwcompany.fortune_index.saju.SajuAnalyzer
import com.hwcompany.fortune_index.saju.SajuPersistenceService
import com.hwcompany.fortune_index.saju.SajuResultRepository
import com.hwcompany.fortune_index.tarot.TarotDeckVersionRepository
import java.time.LocalDate
import java.util.Optional
import kotlin.test.Test
import kotlin.test.assertEquals
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`
import org.springframework.beans.factory.support.StaticListableBeanFactory
import org.springframework.security.crypto.password.PasswordEncoder

class AuthServiceTest {
    private val userRepository = mock(UserRepository::class.java)
    private val virtualInvestmentRepository = mock(VirtualInvestmentRepository::class.java)
    private val refreshTokenRepository = mock(RefreshTokenRepository::class.java)
    private val emailVerificationTokenRepository = mock(EmailVerificationTokenRepository::class.java)
    private val emailVerificationRepository = mock(EmailVerificationRepository::class.java)
    private val passwordEncoder = mock(PasswordEncoder::class.java)
    private val tarotDeckVersionRepository = mock(TarotDeckVersionRepository::class.java)
    private val sajuResultRepository = mock(SajuResultRepository::class.java)
    private val authProperties = AuthProperties()
    private val jwtTokenService = JwtTokenService(authProperties)
    private val sajuPersistenceService = SajuPersistenceService(
        sajuAnalyzer = SajuAnalyzer(),
        sajuResultRepository = sajuResultRepository,
        userRepository = userRepository
    )

    private val service = AuthService(
        userRepository = userRepository,
        virtualInvestmentRepository = virtualInvestmentRepository,
        sajuPersistenceService = sajuPersistenceService,
        refreshTokenRepository = refreshTokenRepository,
        emailVerificationTokenRepository = emailVerificationTokenRepository,
        emailVerificationRepository = emailVerificationRepository,
        passwordEncoder = passwordEncoder,
        jwtTokenService = jwtTokenService,
        authProperties = authProperties,
        tarotDeckVersionRepository = tarotDeckVersionRepository,
        mailSenderProvider = StaticListableBeanFactory().getBeanProvider(org.springframework.mail.javamail.JavaMailSender::class.java)
    )

    @Test
    fun `내 정보 수정은 투자 성향을 갱신한다`() {
        val user = User(
            id = 1L,
            name = "Tester",
            email = "tester@example.com",
            passwordHash = "encoded-password",
            birthInfo = BirthInfo(
                birthDate = LocalDate.of(1990, 1, 1),
                birthTime = null
            ),
            accountStatus = UserAccountStatus.ACTIVE,
            emailVerified = true,
            gender = UserGender.M,
            preferredTarotDeckId = "main",
            investmentRiskProfile = InvestmentRiskProfile.STABLE,
            subscriptionTier = SubscriptionTier.FREE
        )
        `when`(userRepository.findById(1L)).thenReturn(Optional.of(user))
        `when`(virtualInvestmentRepository.existsByUserId(1L)).thenReturn(false)

        val response = service.updateCurrentUser(
            authenticatedUser = AuthenticatedUser(userId = 1L, email = "tester@example.com"),
            request = UpdateCurrentUserRequest(investmentRiskProfile = InvestmentRiskProfile.AGGRESSIVE)
        )

        assertEquals(InvestmentRiskProfile.AGGRESSIVE, user.investmentRiskProfile)
        assertEquals(InvestmentRiskProfile.AGGRESSIVE, response.investmentRiskProfile)
    }

    @Test
    fun `내 정보 수정은 선호 섹터를 갱신하고 응답에 반영한다`() {
        val user = User(
            id = 1L,
            name = "Tester",
            email = "tester@example.com",
            passwordHash = "encoded-password",
            birthInfo = BirthInfo(
                birthDate = LocalDate.of(1990, 1, 1),
                birthTime = null
            ),
            accountStatus = UserAccountStatus.ACTIVE,
            emailVerified = true,
            gender = UserGender.M,
            preferredTarotDeckId = "main",
            investmentRiskProfile = InvestmentRiskProfile.STABLE,
            subscriptionTier = SubscriptionTier.FREE,
            preferredSectors = mutableSetOf(InvestmentSector.ENERGY)
        )
        `when`(userRepository.findById(1L)).thenReturn(Optional.of(user))
        `when`(virtualInvestmentRepository.existsByUserId(1L)).thenReturn(false)

        val response = service.updateCurrentUser(
            authenticatedUser = AuthenticatedUser(userId = 1L, email = "tester@example.com"),
            request = UpdateCurrentUserRequest(
                preferredSectors = setOf(InvestmentSector.FINANCE, InvestmentSector.ETF)
            )
        )

        assertEquals(
            setOf(InvestmentSector.FINANCE, InvestmentSector.ETF),
            user.preferredSectors
        )
        assertEquals(
            listOf(InvestmentSector.ETF, InvestmentSector.FINANCE),
            response.preferredSectors
        )
    }
}
