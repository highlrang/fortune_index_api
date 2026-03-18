package com.hwcompany.fortune_index.scheduler

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.JsonNodeFactory
import com.hwcompany.fortune_index.ai.AnalysisSectionPayload
import com.hwcompany.fortune_index.ai.AnalysisResultsPayload
import com.hwcompany.fortune_index.ai.AiProvider
import com.hwcompany.fortune_index.ai.HybridConsultingAiClient
import com.hwcompany.fortune_index.ai.HybridConsultingAiResponse
import com.hwcompany.fortune_index.consulting.AnalysisMode
import com.hwcompany.fortune_index.domain.model.BirthInfo
import com.hwcompany.fortune_index.domain.model.InvestmentRiskProfile
import com.hwcompany.fortune_index.domain.model.User
import com.hwcompany.fortune_index.history.ConsultingHistoryRepository
import com.hwcompany.fortune_index.history.UserRepository
import com.hwcompany.fortune_index.market.MarketDataProvider
import com.hwcompany.fortune_index.market.StockInfo
import com.hwcompany.fortune_index.market.StockService
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.mockito.ArgumentMatchers.any
import org.mockito.ArgumentMatchers.anyString
import org.mockito.BDDMockito.given
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.mock.mockito.MockBean

@SpringBootTest(
    properties = [
        "app.scheduler.daily-fortune.enabled=false",
        "app.scheduler.daily-consulting.enabled=false"
    ]
)
class AutomatedConsultingSchedulerSmokeTest {
    @Autowired
    private lateinit var automatedConsultingSchedulerService: AutomatedConsultingSchedulerService

    @Autowired
    private lateinit var userRepository: UserRepository

    @Autowired
    private lateinit var consultingHistoryRepository: ConsultingHistoryRepository

    @MockBean
    private lateinit var stockService: StockService

    @MockBean
    private lateinit var hybridConsultingAiClient: HybridConsultingAiClient

    @Test
    fun `run STOCK_ALL scheduler once on h2`() {
        val user = userRepository.save(
            User(
                name = "임시 사용자",
                email = "scheduler-smoke@test.local",
                passwordHash = "encoded-password",
                birthInfo = BirthInfo(
                    birthDate = LocalDate.of(1999, 1, 27),
                    birthTime = LocalTime.of(10, 20)
                ),
                emailVerified = true,
                investmentRiskProfile = InvestmentRiskProfile.AGGRESSIVE
            )
        )

        given(stockService.getStockInfo(anyString())).willReturn(
            StockInfo(
                ticker = "005930",
                currentPrice = BigDecimal("71200"),
                changeRate = BigDecimal("1.23"),
                sector = "TECHNOLOGY",
                source = MarketDataProvider.KIS,
                fallback = false
            )
        )
        given(hybridConsultingAiClient.requestJsonAdvice(anyString(), anyJsonNode())).willReturn(
            HybridConsultingAiResponse(
                provider = AiProvider.GEMINI,
                model = "test-model",
                mode = AnalysisMode.STOCK_ALL.name,
                analysisResults = AnalysisResultsPayload(
                    market_analysis = AnalysisSectionPayload(
                        title = "증시 관련 분석",
                        content = "거래량이 붙은 완만한 반등 구간이라 성급한 추격보다 분할 접근이 유리하다."
                    ),
                    tarot_analysis = AnalysisSectionPayload(
                        title = "타로 카드 분석",
                        content = "태양 카드 계열의 흐름이 명확성을 주지만 과열 신호는 경계해야 한다."
                    ),
                    saju_analysis = AnalysisSectionPayload(
                        title = "사주 분석",
                        content = "일간과 세운의 흐름상 무리한 승부보다 규칙 있는 대응이 더 안정적이다."
                    )
                ),
                finalAdvice = "시장과 사주, 타로 흐름이 모두 무리한 승부보다 계획적인 접근이 낫다고 말한다.",
                riskScore = 42,
                rawJson = """
                    {
                      "mode":"STOCK_ALL",
                      "analysis_results":{
                        "market_analysis":{"title":"증시 관련 분석","content":"거래량이 붙은 완만한 반등 구간이라 성급한 추격보다 분할 접근이 유리하다."},
                        "tarot_analysis":{"title":"타로 카드 분석","content":"태양 카드 계열의 흐름이 명확성을 주지만 과열 신호는 경계해야 한다."},
                        "saju_analysis":{"title":"사주 분석","content":"일간과 세운의 흐름상 무리한 승부보다 규칙 있는 대응이 더 안정적이다."}
                      },
                      "overall_summary":"시장과 사주, 타로 흐름이 모두 무리한 승부보다 계획적인 접근이 낫다고 말한다.",
                      "risk_score":42
                    }
                """.trimIndent()
            )
        )

        val result = automatedConsultingSchedulerService.generateDailyConsultings(setOf(AnalysisMode.STOCK_ALL))
        val histories = consultingHistoryRepository.findByUserIdOrderByConsultedAtDesc(requireNotNull(user.id))

        assertThat(result.createdCount).isEqualTo(1)
        assertThat(result.failedCount).isZero()
        assertThat(result.items).hasSize(1)
        assertThat(histories).hasSize(1)

        val item = result.items.single()
        val history = histories.single()

        println("=== SCHEDULER_SMOKE_RESULT ===")
        println("userId=${item.userId}")
        println("mode=${item.mode}")
        println("scenario=${item.scenario}")
        println("question=${item.question}")
        println("historyId=${item.historyId}")
        println("aiSummary=${item.aiSummary}")
        println("storedShareKey=${history.shareKey}")
        println("storedConsultedAt=${history.consultedAt}")
        println("storedTarotSummary=${history.tarotSnapshot.summary}")
        println("storedSajuSummary=${history.sajuSnapshot.summary}")
        println("storedAiResponseJson=${history.aiResponseJson}")
    }

    @Test
    fun `hourly random scheduler can create same mode again in different hour`() {
        val user = userRepository.save(
            User(
                name = "시간별 사용자",
                email = "scheduler-hourly@test.local",
                passwordHash = "encoded-password",
                birthInfo = BirthInfo(
                    birthDate = LocalDate.of(1995, 5, 10),
                    birthTime = LocalTime.of(8, 15)
                ),
                emailVerified = true,
                investmentRiskProfile = InvestmentRiskProfile.STABLE
            )
        )

        given(stockService.getStockInfo(anyString())).willReturn(
            StockInfo(
                ticker = "005930",
                currentPrice = BigDecimal("71200"),
                changeRate = BigDecimal("1.23"),
                sector = "TECHNOLOGY",
                source = MarketDataProvider.KIS,
                fallback = false
            )
        )
        given(hybridConsultingAiClient.requestJsonAdvice(anyString(), anyJsonNode())).willReturn(
            HybridConsultingAiResponse(
                provider = AiProvider.GEMINI,
                model = "test-model",
                mode = AnalysisMode.STOCK_ALL.name,
                analysisResults = AnalysisResultsPayload(
                    market_analysis = AnalysisSectionPayload("증시 관련 분석", "시장 분석"),
                    tarot_analysis = AnalysisSectionPayload("타로 카드 분석", "타로 분석"),
                    saju_analysis = AnalysisSectionPayload("사주 분석", "사주 분석")
                ),
                finalAdvice = "시간별 랜덤 상담 요약",
                riskScore = 35,
                rawJson = """
                    {
                      "mode":"STOCK_ALL",
                      "analysis_results":{
                        "market_analysis":{"title":"증시 관련 분석","content":"시장 분석"},
                        "tarot_analysis":{"title":"타로 카드 분석","content":"타로 분석"},
                        "saju_analysis":{"title":"사주 분석","content":"사주 분석"}
                      },
                      "overall_summary":"시간별 랜덤 상담 요약",
                      "risk_score":35
                    }
                """.trimIndent()
            )
        )

        val first = automatedConsultingSchedulerService.generateHourlyRandomConsultings(
            LocalDateTime.of(2026, 3, 18, 13, 5)
        )
        val second = automatedConsultingSchedulerService.generateHourlyRandomConsultings(
            LocalDateTime.of(2026, 3, 18, 14, 5)
        )

        val histories = consultingHistoryRepository.findByUserIdOrderByConsultedAtDesc(requireNotNull(user.id))

        assertThat(first.createdCount).isEqualTo(1)
        assertThat(second.createdCount).isEqualTo(1)
        assertThat(histories).hasSize(2)
    }

    private fun anyJsonNode(): JsonNode {
        return any(JsonNode::class.java) ?: JsonNodeFactory.instance.objectNode()
    }
}
