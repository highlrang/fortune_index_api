package com.hwcompany.fortune_index.saju

import com.hwcompany.fortune_index.domain.model.BirthInfo
import com.hwcompany.fortune_index.domain.model.InvestmentRiskProfile
import com.hwcompany.fortune_index.domain.model.User
import com.hwcompany.fortune_index.domain.model.UserAccountStatus
import com.hwcompany.fortune_index.history.UserRepository
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate
import java.time.LocalTime

@SpringBootTest
@ActiveProfiles("h2")
@Transactional
class SajuPersistenceServiceTest(
    @Autowired private val userRepository: UserRepository,
    @Autowired private val sajuResultRepository: SajuResultRepository,
    @Autowired private val sajuPersistenceService: SajuPersistenceService
) {
    @Test
    fun `backfill creates missing result only for users without saju result`() {
        val existingUser = userRepository.save(
            user(name = "existing", email = "existing@example.com", day = 1, time = LocalTime.of(9, 0))
        )
        val missingUser = userRepository.save(
            user(name = "missing", email = "missing@example.com", day = 2, time = null)
        )

        sajuPersistenceService.saveInitialResult(existingUser)

        val summary = sajuPersistenceService.backfillMissingResults(batchSize = 10)

        assertThat(summary.scannedUsers).isGreaterThanOrEqualTo(2)
        assertThat(summary.createdResults).isEqualTo(1)
        assertThat(sajuResultRepository.findTopByUserIdOrderByAnalyzedAtDesc(requireNotNull(existingUser.id))).isNotNull
        assertThat(sajuResultRepository.findTopByUserIdOrderByAnalyzedAtDesc(requireNotNull(missingUser.id))).isNotNull
    }

    private fun user(name: String, email: String, day: Int, time: LocalTime?): User =
        User(
            name = name,
            email = email,
            passwordHash = "encoded-password",
            birthInfo = BirthInfo(
                birthDate = LocalDate.of(1990, 1, day),
                birthTime = time
            ),
            accountStatus = UserAccountStatus.ACTIVE,
            emailVerified = true,
            investmentRiskProfile = InvestmentRiskProfile.STABLE
        )
}
