package com.hwcompany.fortune_index.investing

import com.hwcompany.fortune_index.consulting.AnalysisMode
import com.hwcompany.fortune_index.consulting.ConsultingRiskScoreCalculator
import com.hwcompany.fortune_index.consulting.ConsultingScenario
import com.hwcompany.fortune_index.consulting.InvestmentDisclaimerConstants
import com.hwcompany.fortune_index.history.UserRepository
import com.hwcompany.fortune_index.saju.SajuAnalyzer
import com.hwcompany.fortune_index.saju.investment.SajuInvestmentFeatureService
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.web.server.ResponseStatusException

@Service
class DailyInvestmentRiskIndexService(
    private val userRepository: UserRepository,
    private val sajuAnalyzer: SajuAnalyzer,
    private val sajuInvestmentFeatureService: SajuInvestmentFeatureService,
    private val consultingRiskScoreCalculator: ConsultingRiskScoreCalculator
) {
    fun computeForUser(userId: Long): DailyInvestmentRiskIndexResponse {
        val user = userRepository.findById(userId)
            .orElseThrow { ResponseStatusException(HttpStatus.NOT_FOUND, "user not found: $userId") }

        val now = LocalDateTime.now(SEOUL_ZONE_ID)
        val birthDateTime = LocalDateTime.of(
            user.birthInfo.birthDate,
            user.birthInfo.birthTime ?: LocalTime.NOON
        )

        val saju = sajuAnalyzer.analyzeForConsulting(
            birthDateTime = birthDateTime,
            referenceDateTime = now,
            zoneId = SEOUL_ZONE_ID,
            gender = user.gender
        )
        val sajuFeatures = sajuInvestmentFeatureService.extract(saju)

        val riskScore = consultingRiskScoreCalculator.calculate(
            mode = AnalysisMode.INVESTMENT_ALL,
            scenario = ConsultingScenario.MENTAL_GUIDE,
            riskProfile = user.investmentRiskProfile,
            sajuFeatures = sajuFeatures
        )

        val energyLabel = energyLabel(riskScore)

        return DailyInvestmentRiskIndexResponse(
            userId = userId,
            date = now.toLocalDate(),
            riskScore = riskScore,
            energyLabel = energyLabel,
            riskFlags = sajuFeatures.riskFlags,
            disclaimer = InvestmentDisclaimerConstants.LEGAL_DISCLAIMER
        )
    }

    private fun energyLabel(riskScore: Int): String =
        when {
            riskScore >= 70 -> "긴장 기운이 높은 날 — 속도보다 방향을 먼저 살피세요"
            riskScore >= 50 -> "흐름을 살피기 좋은 날 — 차분히 점검해 보세요"
            riskScore >= 30 -> "안정적인 기운의 날 — 균형 있게 나아가세요"
            else -> "에너지를 모으는 날 — 쉬어가는 흐름에 가깝습니다"
        }

    companion object {
        private val SEOUL_ZONE_ID: ZoneId = ZoneId.of("Asia/Seoul")
    }
}

data class DailyInvestmentRiskIndexResponse(
    val userId: Long,
    val date: LocalDate,
    val riskScore: Int,
    val energyLabel: String,
    val riskFlags: List<String>,
    val disclaimer: String
)
