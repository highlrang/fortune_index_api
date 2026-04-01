package com.hwcompany.fortune_index.consulting

import com.fasterxml.jackson.databind.ObjectMapper
import com.hwcompany.fortune_index.ai.AiChatRequest
import com.hwcompany.fortune_index.ai.StockFortuneAdviceResponse
import com.hwcompany.fortune_index.ai.StockFortuneAdviceService
import com.hwcompany.fortune_index.consulting.prompt.LlmPromptCode
import com.hwcompany.fortune_index.consulting.prompt.LlmPromptTemplateService
import com.hwcompany.fortune_index.market.StockInfo
import com.hwcompany.fortune_index.market.StockService
import com.hwcompany.fortune_index.saju.FiveElement
import com.hwcompany.fortune_index.saju.FiveElementBalance
import com.hwcompany.fortune_index.saju.Pillar
import com.hwcompany.fortune_index.saju.SajuAnalysisResult
import com.hwcompany.fortune_index.saju.SajuAnalyzer
import com.hwcompany.fortune_index.saju.SajuCharacter
import com.hwcompany.fortune_index.saju.TenGod
import java.math.BigDecimal
import java.time.LocalDateTime
import java.time.ZoneId
import org.springframework.stereotype.Service

@Service
class AdvancedConsultingService(
    private val sajuAnalyzer: SajuAnalyzer,
    private val stockService: StockService,
    private val stockFortuneAdviceService: StockFortuneAdviceService,
    private val objectMapper: ObjectMapper,
    private val llmPromptTemplateService: LlmPromptTemplateService
) {
    fun requestConsulting(request: AdvancedConsultingRequest): AdvancedConsultingResponse {
        val stock = stockService.getStockInfo(request.stockCode)
        val sajuAnalysis = sajuAnalyzer.analyze(
            birthDateTime = request.birthDateTime,
            majorFortunePillar = request.majorFortunePillar,
            referenceDateTime = request.referenceDateTime,
            zoneId = request.zoneId
        )

        val context = AdvancedConsultingContext(
            userName = request.userName,
            stock = StockContext.from(stock),
            sajuCore = buildSajuCoreSummary(sajuAnalysis),
            tenGodProfile = buildTenGodSummary(sajuAnalysis),
            elementBalance = buildElementBalanceSummary(sajuAnalysis.fiveElementBalance),
            fortuneFlow = buildFortuneSummary(sajuAnalysis),
            investmentStyle = request.investmentStyle.description,
            rawSajuAnalysis = sajuAnalysis
        )

        val advice = stockFortuneAdviceService.generateAdvice(
            AiChatRequest(
                systemPersona = buildString {
                    append(llmPromptTemplateService.getContent(LlmPromptCode.ADVANCED_SYSTEM_DEFAULT))
                    append('\n')
                    append(InvestmentPartnerPersonaPromptGuidance.build())
                    append('\n')
                    append(MarketEvidencePromptGuidance.build())
                },
                userMessage = objectMapper.writeValueAsString(
                    mapOf(
                        "userName" to context.userName,
                        "marketContext" to stock.toSectorMarketContext(),
                        "marketPhenomenon" to stock.toSectorMarketContext().toMarketPhenomenonContext(),
                        "focusArea" to stock.sector,
                        "sajuCore" to context.sajuCore,
                        "tenGodProfile" to context.tenGodProfile,
                        "elementBalance" to context.elementBalance,
                        "fortuneFlow" to context.fortuneFlow,
                        "investmentStyle" to context.investmentStyle,
                        "analysis" to context.rawSajuAnalysis,
                        "question" to (request.question ?: llmPromptTemplateService.getContent(LlmPromptCode.ADVANCED_QUESTION_DEFAULT))
                    )
                )
            )
        )

        return AdvancedConsultingResponse(
            advice = advice,
            context = context
        )
    }

    private fun buildSajuCoreSummary(analysis: SajuAnalysisResult): String {
        val dayMaster = analysis.keyPalaces.dayMaster
        val dayBranch = analysis.keyPalaces.dayBranch
        val monthBranch = analysis.keyPalaces.monthBranch
        val frame = determineFrame(dayMaster, monthBranch)
        return "${formatStem(dayMaster)} 일간, ${formatBranch(dayBranch)} 일지, ${formatBranch(monthBranch)} 월지로 구성된 $frame"
    }

    private fun determineFrame(dayMaster: SajuCharacter, monthBranch: SajuCharacter): String {
        return when {
            dayMaster.fiveElement == monthBranch.fiveElement -> "건록격"
            SajuAnalyzer.GENERATES.getValue(monthBranch.fiveElement) == dayMaster.fiveElement -> "인수격"
            SajuAnalyzer.GENERATES.getValue(dayMaster.fiveElement) == monthBranch.fiveElement -> "식신격"
            SajuAnalyzer.CONTROLS.getValue(dayMaster.fiveElement) == monthBranch.fiveElement -> "재성격"
            else -> "관성격"
        }
    }

    private fun buildTenGodSummary(analysis: SajuAnalysisResult): String {
        val dominantTenGod = analysis.tenGods
            .groupingBy { it.tenGod }
            .eachCount()
            .maxByOrNull { it.value }
            ?.key
            ?: return "십성 편중이 크지 않아 재물 감각이 비교적 균형적인 편"

        return "사주에 ${formatTenGod(dominantTenGod)}가 강함"
    }

    private fun buildElementBalanceSummary(balance: FiveElementBalance): String {
        val elements = linkedMapOf(
            FiveElement.WOOD to balance.wood,
            FiveElement.FIRE to balance.fire,
            FiveElement.EARTH to balance.earth,
            FiveElement.METAL to balance.metal,
            FiveElement.WATER to balance.water
        )
        val max = elements.maxByOrNull { it.value } ?: return "오행 분포를 계산할 수 없음"
        val min = elements.minByOrNull { it.value } ?: return "오행 분포를 계산할 수 없음"

        if (max.value == min.value) {
            return "오행이 전체적으로 고르게 분포함"
        }

        return "${formatElement(min.key)} 기운이 부족하고 ${formatElement(max.key)} 기운이 강함"
    }

    private fun buildFortuneSummary(analysis: SajuAnalysisResult): String {
        val yearly = formatTenGod(analysis.annualFortune.stemTenGod)
        val major = formatTenGod(analysis.majorFortune.stemTenGod)
        val yearlyInsight = when (analysis.annualFortune.stemTenGod) {
            TenGod.PYEONJAE, TenGod.JEONGJAE -> "재물의 흐름을 차분히 살피면 안쪽 감각이 살아나기 쉬운 흐름"
            TenGod.SIKSIN, TenGod.SANGGWAN -> "아이디어와 실행력이 마음의 자신감으로 이어지기 쉬운 흐름"
            TenGod.PYEONGWAN, TenGod.JEONGGWAN -> "압박이 커질수록 중심과 호흡을 지키는 일이 중요한 흐름"
            TenGod.BIGYEON, TenGod.GEOPJAE -> "경쟁 심리가 과열되기 쉬워 마음의 과속을 경계해야 하는 흐름"
            TenGod.PYEONIN, TenGod.JEONGIN -> "학습과 관찰을 통해 내 감각을 정리하기 좋은 흐름"
        }

        return "세운은 $yearly, 대운은 $major 흐름이며 $yearlyInsight"
    }

    private fun formatStem(character: SajuCharacter): String {
        val stem = requireNotNull(character.referenceStem)
        val property = SajuAnalyzer.STEM_PROPERTIES.getValue(stem)
        return "${stemKoreanName(stem)}${elementKoreanSuffix(property.element)}(${stemHanja(stem)})"
    }

    private fun formatBranch(character: SajuCharacter): String {
        return "${branchKoreanName(character.symbol)}${elementKoreanSuffix(character.fiveElement)}(${branchHanja(character.symbol)})"
    }

    private fun formatElement(element: FiveElement): String =
        when (element) {
            FiveElement.WOOD -> "목(木)"
            FiveElement.FIRE -> "화(火)"
            FiveElement.EARTH -> "토(土)"
            FiveElement.METAL -> "금(金)"
            FiveElement.WATER -> "수(水)"
        }

    private fun formatTenGod(tenGod: TenGod): String =
        when (tenGod) {
            TenGod.BIGYEON -> "비견(자기 확신)"
            TenGod.GEOPJAE -> "겁재(경쟁 자본)"
            TenGod.SIKSIN -> "식신(꾸준한 생산성)"
            TenGod.SANGGWAN -> "상관(공격적 돌파)"
            TenGod.PYEONJAE -> "편재(역마성 재물)"
            TenGod.JEONGJAE -> "정재(안정형 재물)"
            TenGod.PYEONGWAN -> "편관(승부와 압박)"
            TenGod.JEONGGWAN -> "정관(질서와 규율)"
            TenGod.PYEONIN -> "편인(직감과 변칙)"
            TenGod.JEONGIN -> "정인(학습과 분석)"
        }

    private fun stemKoreanName(stem: com.hwcompany.fortune_index.domain.model.HeavenlyStem): String =
        when (stem) {
            com.hwcompany.fortune_index.domain.model.HeavenlyStem.GAP -> "갑"
            com.hwcompany.fortune_index.domain.model.HeavenlyStem.EUL -> "을"
            com.hwcompany.fortune_index.domain.model.HeavenlyStem.BYEONG -> "병"
            com.hwcompany.fortune_index.domain.model.HeavenlyStem.JEONG -> "정"
            com.hwcompany.fortune_index.domain.model.HeavenlyStem.MU -> "무"
            com.hwcompany.fortune_index.domain.model.HeavenlyStem.GI -> "기"
            com.hwcompany.fortune_index.domain.model.HeavenlyStem.GYEONG -> "경"
            com.hwcompany.fortune_index.domain.model.HeavenlyStem.SIN -> "신"
            com.hwcompany.fortune_index.domain.model.HeavenlyStem.IM -> "임"
            com.hwcompany.fortune_index.domain.model.HeavenlyStem.GYE -> "계"
        }

    private fun stemHanja(stem: com.hwcompany.fortune_index.domain.model.HeavenlyStem): String =
        when (stem) {
            com.hwcompany.fortune_index.domain.model.HeavenlyStem.GAP -> "甲"
            com.hwcompany.fortune_index.domain.model.HeavenlyStem.EUL -> "乙"
            com.hwcompany.fortune_index.domain.model.HeavenlyStem.BYEONG -> "丙"
            com.hwcompany.fortune_index.domain.model.HeavenlyStem.JEONG -> "丁"
            com.hwcompany.fortune_index.domain.model.HeavenlyStem.MU -> "戊"
            com.hwcompany.fortune_index.domain.model.HeavenlyStem.GI -> "己"
            com.hwcompany.fortune_index.domain.model.HeavenlyStem.GYEONG -> "庚"
            com.hwcompany.fortune_index.domain.model.HeavenlyStem.SIN -> "辛"
            com.hwcompany.fortune_index.domain.model.HeavenlyStem.IM -> "壬"
            com.hwcompany.fortune_index.domain.model.HeavenlyStem.GYE -> "癸"
        }

    private fun branchKoreanName(branchSymbol: String): String =
        when (branchSymbol) {
            "JA" -> "자"
            "CHUK" -> "축"
            "IN" -> "인"
            "MYO" -> "묘"
            "JIN" -> "진"
            "SA" -> "사"
            "O" -> "오"
            "MI" -> "미"
            "SIN" -> "신"
            "YU" -> "유"
            "SUL" -> "술"
            else -> "해"
        }

    private fun branchHanja(branchSymbol: String): String =
        when (branchSymbol) {
            "JA" -> "子"
            "CHUK" -> "丑"
            "IN" -> "寅"
            "MYO" -> "卯"
            "JIN" -> "辰"
            "SA" -> "巳"
            "O" -> "午"
            "MI" -> "未"
            "SIN" -> "申"
            "YU" -> "酉"
            "SUL" -> "戌"
            else -> "亥"
        }

    private fun elementKoreanSuffix(element: FiveElement): String =
        when (element) {
            FiveElement.WOOD -> "목"
            FiveElement.FIRE -> "화"
            FiveElement.EARTH -> "토"
            FiveElement.METAL -> "금"
            FiveElement.WATER -> "수"
        }

}

data class AdvancedConsultingRequest(
    val userId: Long,
    val userName: String,
    val birthDateTime: LocalDateTime,
    val majorFortunePillar: Pillar,
    val investmentStyle: InvestmentStyle,
    val stockCode: String,
    val question: String? = null,
    val referenceDateTime: LocalDateTime = LocalDateTime.now(ZoneId.of("Asia/Seoul")),
    val zoneId: ZoneId = ZoneId.of("Asia/Seoul")
)

data class AdvancedConsultingResponse(
    val advice: StockFortuneAdviceResponse,
    val context: AdvancedConsultingContext
)

data class AdvancedConsultingContext(
    val userName: String,
    val stock: StockContext,
    val sajuCore: String,
    val tenGodProfile: String,
    val elementBalance: String,
    val fortuneFlow: String,
    val investmentStyle: String,
    val rawSajuAnalysis: SajuAnalysisResult
)

data class StockContext(
    val ticker: String,
    val currentPrice: BigDecimal,
    val changeRate: BigDecimal,
    val sector: String,
    val fallback: Boolean,
    val marketDataAsOf: java.time.LocalDate,
    val marketNarrative: String,
    val tradingSnapshot: com.hwcompany.fortune_index.market.TradingSnapshot,
    val fundamentals: com.hwcompany.fortune_index.market.FundamentalSnapshot
) {
    companion object {
        fun from(stockInfo: StockInfo): StockContext =
            StockContext(
                ticker = stockInfo.ticker,
                currentPrice = stockInfo.currentPrice,
                changeRate = stockInfo.changeRate,
                sector = stockInfo.sector,
                fallback = stockInfo.fallback,
                marketDataAsOf = stockInfo.marketDataAsOf,
                marketNarrative = stockInfo.marketNarrative,
                tradingSnapshot = stockInfo.tradingSnapshot,
                fundamentals = stockInfo.fundamentals
            )
    }
}

enum class InvestmentStyle(val description: String) {
    STABLE("안정형"),
    AGGRESSIVE("공격형")
}
