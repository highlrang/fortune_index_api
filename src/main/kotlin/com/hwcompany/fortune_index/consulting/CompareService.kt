package com.hwcompany.fortune_index.consulting

import com.fasterxml.jackson.databind.ObjectMapper
import com.hwcompany.fortune_index.ai.AiChatRequest
import com.hwcompany.fortune_index.ai.StockFortuneAdviceResponse
import com.hwcompany.fortune_index.ai.StockFortuneAdviceService
import com.hwcompany.fortune_index.investment.VirtualInvestmentService
import com.hwcompany.fortune_index.market.StockInfo
import com.hwcompany.fortune_index.market.StockService
import com.hwcompany.fortune_index.saju.FiveElement
import com.hwcompany.fortune_index.saju.FiveElementBalance
import com.hwcompany.fortune_index.saju.Pillar
import com.hwcompany.fortune_index.saju.SajuAnalysisResult
import com.hwcompany.fortune_index.saju.SajuAnalyzer
import com.hwcompany.fortune_index.saju.SajuCharacter
import com.hwcompany.fortune_index.saju.TenGod
import com.hwcompany.fortune_index.tarot.TarotCard
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.LocalDateTime
import java.time.ZoneId
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.web.server.ResponseStatusException
import com.hwcompany.fortune_index.consulting.prompt.PromptProvider

@Service
class CompareService(
    private val stockService: StockService,
    private val stockFortuneAdviceService: StockFortuneAdviceService,
    private val sajuAnalyzer: SajuAnalyzer,
    private val virtualInvestmentService: VirtualInvestmentService,
    promptProviders: List<PromptProvider>,
    private val objectMapper: ObjectMapper
) {
    private val promptProviderByMode: Map<AnalysisMode, PromptProvider> =
        AnalysisMode.entries.associateWith { mode ->
            promptProviders.firstOrNull { it.supports(mode) }
                ?: error("PromptProvider is missing for mode=$mode")
        }

    fun compare(request: CompareRequest): CompareResponse {
        validateRequest(request)

        val stock = stockService.getStockInfo(request.stockCode)
        val investmentSnapshot = summarizeVirtualInvestment(request.userId, stock.ticker)

        val results = request.modes.map { mode ->
            val sajuAnalysis = if (mode.includesSaju()) {
                sajuAnalyzer.analyze(
                    birthDateTime = requireNotNull(request.birthDateTime),
                    majorFortunePillar = requireNotNull(request.majorFortunePillar),
                    referenceDateTime = request.referenceDateTime,
                    zoneId = request.zoneId
                )
            } else {
                null
            }

            val payload = buildPayload(
                request = request,
                mode = mode,
                stock = stock,
                investmentSnapshot = investmentSnapshot,
                sajuAnalysis = sajuAnalysis,
                tarotCard = request.tarotCard.takeIf { mode.includesTarot() }
            )
            val promptProvider = promptProviderByMode.getValue(mode)
            val response = stockFortuneAdviceService.generateAdvice(
                AiChatRequest(
                    systemPersona = promptProvider.systemMessage(),
                    userMessage = objectMapper.writeValueAsString(payload)
                )
            )

            CompareModeResult(
                mode = mode,
                label = mode.name,
                systemMessage = promptProvider.systemMessage(),
                response = response
            )
        }

        return CompareResponse(results = results)
    }

    private fun buildPayload(
        request: CompareRequest,
        mode: AnalysisMode,
        stock: StockInfo,
        investmentSnapshot: CompareInvestmentSnapshot,
        sajuAnalysis: SajuAnalysisResult?,
        tarotCard: TarotCard?
    ): Map<String, Any?> {
        val payload = linkedMapOf<String, Any?>(
            "mode" to mode.name,
            "userName" to request.userName,
            "investmentStyle" to request.investmentStyle.description,
            "stock" to mapOf(
                "ticker" to stock.ticker,
                "currentPrice" to stock.currentPrice,
                "changeRate" to stock.changeRate,
                "sector" to stock.sector,
                "fallback" to stock.fallback
            ),
            "virtualInvestment" to mapOf(
                "summary" to investmentSnapshot.summary,
                "returnRate" to investmentSnapshot.returnRate
            ),
            "question" to (request.question ?: defaultQuestion(mode))
        )

        if (sajuAnalysis != null) {
            payload["saju"] = mapOf(
                "coreSummary" to buildSajuCoreSummary(sajuAnalysis),
                "tenGodSummary" to buildTenGodSummary(sajuAnalysis),
                "elementSummary" to buildElementBalanceSummary(sajuAnalysis.fiveElementBalance),
                "fortuneSummary" to buildFortuneSummary(sajuAnalysis),
                "analysis" to sajuAnalysis
            )
        }

        if (tarotCard != null) {
            payload["tarot"] = mapOf(
                "cardCode" to tarotCard.code,
                "cardNumber" to tarotCard.cardNumber,
                "displayName" to tarotCard.displayName,
                "arcanaType" to tarotCard.arcanaType.name,
                "suit" to tarotCard.suit?.name,
                "meaning" to tarotCard.uprightMeaning
            )
        }

        return payload
    }

    private fun summarizeVirtualInvestment(userId: Long, stockCode: String): CompareInvestmentSnapshot {
        val positions = virtualInvestmentService.getUserVirtualInvestments(userId, holdingOnly = true)
            .filter { it.stockCode.equals(stockCode, ignoreCase = true) }

        if (positions.isEmpty()) {
            return CompareInvestmentSnapshot(
                returnRate = null,
                summary = "현재 보유 중인 해당 모의투자 종목이 없어 비교용 수익률은 없음"
            )
        }

        val totalBuyAmount = positions.fold(BigDecimal.ZERO) { acc, position ->
            acc + position.averageBuyPrice.multiply(BigDecimal.valueOf(position.buyQuantity))
        }
        val totalProfit = positions.fold(BigDecimal.ZERO) { acc, position ->
            acc + position.evaluationProfit
        }
        val returnRate = if (totalBuyAmount.signum() == 0) {
            BigDecimal.ZERO
        } else {
            totalProfit.multiply(HUNDRED)
                .divide(totalBuyAmount, 2, RoundingMode.HALF_UP)
        }

        return CompareInvestmentSnapshot(
            returnRate = returnRate,
            summary = "현재 평단가 대비 수익률 ${returnRate.toPlainString()}%"
        )
    }

    private fun validateRequest(request: CompareRequest) {
        if (request.modes.isEmpty()) {
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "at least one analysis mode is required")
        }
        if (request.modes.any { it.includesSaju() } && (request.birthDateTime == null || request.majorFortunePillar == null)) {
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "birthDateTime and majorFortunePillar are required for saju modes")
        }
        if (request.modes.any { it.includesTarot() } && request.tarotCard == null) {
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "tarotCard is required for tarot modes")
        }
    }

    private fun defaultQuestion(mode: AnalysisMode): String =
        when (mode) {
            AnalysisMode.ONLY_STOCK -> "증시 지표만 보고 현재 대응 전략을 말해줘."
            AnalysisMode.STOCK_SAJU -> "증시와 사주를 같이 보고 장기 흐름을 말해줘."
            AnalysisMode.STOCK_TAROT -> "증시와 타로를 같이 보고 현재 심리와 타이밍을 말해줘."
            AnalysisMode.STOCK_ALL -> "증시, 사주, 타로를 모두 합쳐 현재 전략을 말해줘."
        }

    private fun buildSajuCoreSummary(analysis: SajuAnalysisResult): String {
        val dayMaster = analysis.keyPalaces.dayMaster
        val dayBranch = analysis.keyPalaces.dayBranch
        val monthBranch = analysis.keyPalaces.monthBranch
        val frame = determineFrame(dayMaster, monthBranch)
        return "${formatStem(dayMaster)} 일간, ${formatBranch(dayBranch)} 일지, ${formatBranch(monthBranch)} 월지의 $frame"
    }

    private fun determineFrame(dayMaster: SajuCharacter, monthBranch: SajuCharacter): String =
        when {
            dayMaster.fiveElement == monthBranch.fiveElement -> "건록격"
            SajuAnalyzer.GENERATES.getValue(monthBranch.fiveElement) == dayMaster.fiveElement -> "인수격"
            SajuAnalyzer.GENERATES.getValue(dayMaster.fiveElement) == monthBranch.fiveElement -> "식신격"
            SajuAnalyzer.CONTROLS.getValue(dayMaster.fiveElement) == monthBranch.fiveElement -> "재성격"
            else -> "관성격"
        }

    private fun buildTenGodSummary(analysis: SajuAnalysisResult): String {
        val dominant = analysis.tenGods.groupingBy { it.tenGod }.eachCount().maxByOrNull { it.value }?.key
            ?: return "십성 분포가 비교적 고른 편"
        return "사주에 ${formatTenGod(dominant)}가 강한 편"
    }

    private fun buildElementBalanceSummary(balance: FiveElementBalance): String {
        val elements = listOf(
            FiveElement.WOOD to balance.wood,
            FiveElement.FIRE to balance.fire,
            FiveElement.EARTH to balance.earth,
            FiveElement.METAL to balance.metal,
            FiveElement.WATER to balance.water
        )
        val strongest = elements.maxByOrNull { it.second } ?: return "오행 분포 없음"
        val weakest = elements.minByOrNull { it.second } ?: return "오행 분포 없음"
        return if (strongest.second == weakest.second) {
            "오행이 전체적으로 균형적임"
        } else {
            "${formatElement(weakest.first)}가 약하고 ${formatElement(strongest.first)}가 강함"
        }
    }

    private fun buildFortuneSummary(analysis: SajuAnalysisResult): String =
        "세운은 ${formatTenGod(analysis.annualFortune.stemTenGod)}, 대운은 ${formatTenGod(analysis.majorFortune.stemTenGod)} 흐름"

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
            TenGod.BIGYEON -> "비견"
            TenGod.GEOPJAE -> "겁재"
            TenGod.SIKSIN -> "식신"
            TenGod.SANGGWAN -> "상관"
            TenGod.PYEONJAE -> "편재"
            TenGod.JEONGJAE -> "정재"
            TenGod.PYEONGWAN -> "편관"
            TenGod.JEONGGWAN -> "정관"
            TenGod.PYEONIN -> "편인"
            TenGod.JEONGIN -> "정인"
        }

    private fun formatStem(character: SajuCharacter): String {
        val stem = requireNotNull(character.referenceStem)
        val property = SajuAnalyzer.STEM_PROPERTIES.getValue(stem)
        return "${stem.toKorean()}${property.element.toSuffix()}(${stem.toHanja()})"
    }

    private fun formatBranch(character: SajuCharacter): String =
        "${character.symbol.toBranchKorean()}${character.fiveElement.toSuffix()}(${character.symbol.toBranchHanja()})"

    private companion object {
        val HUNDRED: BigDecimal = BigDecimal("100")
    }
}

data class CompareRequest(
    val userId: Long,
    val userName: String,
    val stockCode: String,
    val investmentStyle: InvestmentStyle,
    val modes: Set<AnalysisMode> = AnalysisMode.entries.toSet(),
    val birthDateTime: LocalDateTime? = null,
    val majorFortunePillar: Pillar? = null,
    val tarotCard: TarotCard? = null,
    val question: String? = null,
    val referenceDateTime: LocalDateTime = LocalDateTime.now(ZoneId.of("Asia/Seoul")),
    val zoneId: ZoneId = ZoneId.of("Asia/Seoul")
)

data class CompareResponse(
    val results: List<CompareModeResult>
)

data class CompareModeResult(
    val mode: AnalysisMode,
    val label: String,
    val systemMessage: String,
    val response: StockFortuneAdviceResponse
)

data class CompareInvestmentSnapshot(
    val returnRate: BigDecimal?,
    val summary: String
)

private fun com.hwcompany.fortune_index.domain.model.HeavenlyStem.toKorean(): String =
    when (this) {
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

private fun com.hwcompany.fortune_index.domain.model.HeavenlyStem.toHanja(): String =
    when (this) {
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

private fun String.toBranchKorean(): String =
    when (this) {
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

private fun String.toBranchHanja(): String =
    when (this) {
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

private fun FiveElement.toSuffix(): String =
    when (this) {
        FiveElement.WOOD -> "목"
        FiveElement.FIRE -> "화"
        FiveElement.EARTH -> "토"
        FiveElement.METAL -> "금"
        FiveElement.WATER -> "수"
    }
