package com.hwcompany.fortune_index.auth

import com.hwcompany.fortune_index.domain.model.BirthInfo
import com.hwcompany.fortune_index.domain.model.InvestmentRiskProfile
import com.hwcompany.fortune_index.domain.model.User
import com.hwcompany.fortune_index.domain.model.UserAccountStatus
import com.hwcompany.fortune_index.domain.model.UserGender
import com.hwcompany.fortune_index.history.UserRepository
import com.hwcompany.fortune_index.saju.SajuPersistenceService
import com.hwcompany.fortune_index.tarot.DEFAULT_TAROT_DECK_VERSION_ID
import org.slf4j.LoggerFactory
import org.springframework.boot.ApplicationArguments
import org.springframework.boot.ApplicationRunner
import org.springframework.context.annotation.Profile
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
@Profile("h2")
class H2DefaultUserSeeder(
    private val userRepository: UserRepository,
    private val passwordEncoder: PasswordEncoder,
    private val sajuPersistenceService: SajuPersistenceService,
    private val devSeedUserProperties: DevSeedUserProperties
) : ApplicationRunner {
    @Transactional
    override fun run(args: ApplicationArguments?) {
        if (!devSeedUserProperties.enabled) {
            return
        }

        val normalizedEmail = devSeedUserProperties.email.trim().lowercase()
        if (userRepository.existsByEmail(normalizedEmail)) {
            logger.info("H2 default user already exists. email={}", normalizedEmail)
            return
        }

        val user = userRepository.save(
            User(
                name = devSeedUserProperties.name.trim(),
                email = normalizedEmail,
                passwordHash = passwordEncoder.encode(devSeedUserProperties.password),
                birthInfo = BirthInfo(
                    birthDate = devSeedUserProperties.birthDate,
                    birthTime = devSeedUserProperties.birthTime
                ),
                accountStatus = UserAccountStatus.ACTIVE,
                emailVerified = true,
                gender = UserGender.M,
                preferredTarotDeckId = DEFAULT_TAROT_DECK_VERSION_ID,
                investmentRiskProfile = InvestmentRiskProfile.STABLE
            )
        )

        sajuPersistenceService.saveInitialResult(user)

        logger.info(
            "Seeded H2 default user. email={}, password={}",
            normalizedEmail,
            devSeedUserProperties.password
        )
    }

    companion object {
        private val logger = LoggerFactory.getLogger(H2DefaultUserSeeder::class.java)
    }
}
