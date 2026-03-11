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

@Service
class ComparativeConsultingService(
    private val stockService: StockService,
    private val stockFortuneAdviceService: StockFortuneAdviceService,
    private val sajuAnalyzer: SajuAnalyzer,
    private val virtualInvestmentService: VirtualInvestmentService,
    private val objectMapper: ObjectMapper
) {
    fun consult(request: ComparativeConsultingRequest): ComparativeConsultingResponse {
        validateRequest(request)
        val stock = stockService.getStockInfo(request.stockCode)
        val sharedInvestmentSummary = summarizeVirtualInvestment(request.userId, stock.ticker)
        val modeResponses = request.modes.associateWith { mode ->
            buildModeResponse(
                mode = mode,
                request = request,
                stock = stock,
                investmentSummary = sharedInvestmentSummary
            )
        }

        return ComparativeConsultingResponse(
            stock = StockContext.from(stock),
            virtualInvestment = sharedInvestmentSummary,
            results = modeResponses
        )
    }

    private fun buildModeResponse(
        mode: ConsultingMode,
        request: ComparativeConsultingRequest,
        stock: StockInfo,
        investmentSummary: VirtualInvestmentSnapshot
    ): ModeConsultingResult {
        val sajuAnalysis = if (mode.includesSaju()) {
            sajuAnalyzer.analyze(
                birthDateTime = requireNotNull(request.birthDateTime) { "birthDateTime is required for saju modes" },
                majorFortunePillar = requireNotNull(request.majorFortunePillar) { "majorFortunePillar is required for saju modes" },
                referenceDateTime = request.referenceDateTime,
                zoneId = request.zoneId
            )
        } else {
            null
        }

        val tarotCard = if (mode.includesTarot()) {
            requireNotNull(request.tarotCard) { "tarotCard is required for tarot modes" }
        } else {
            null
        }

        val context = buildContext(
            mode = mode,
            request = request,
            stock = stock,
            sajuAnalysis = sajuAnalysis,
            tarotCard = tarotCard,
            investmentSummary = investmentSummary
        )

        val aiResponse = stockFortuneAdviceService.generateAdvice(
            AiChatRequest(
                systemPersona = buildSystemPersona(mode),
                userMessage = objectMapper.writeValueAsString(context.payload)
            )
        )

        return ModeConsultingResult(
            mode = mode,
            systemPersona = context.systemPersona,
            payload = context.payload,
            response = aiResponse
        )
    }

    private fun buildContext(
        mode: ConsultingMode,
        request: ComparativeConsultingRequest,
        stock: StockInfo,
        sajuAnalysis: SajuAnalysisResult?,
        tarotCard: TarotCard?,
        investmentSummary: VirtualInvestmentSnapshot
    ): ConsultingContextBundle {
        val payload = linkedMapOf<String, Any?>(
            "userName" to request.userName,
            "mode" to mode.name,
            "stock" to mapOf(
                "ticker" to stock.ticker,
                "currentPrice" to stock.currentPrice,
                "changeRate" to stock.changeRate,
                "sector" to stock.sector,
                "fallback" to stock.fallback
            ),
            "investmentStyle" to request.investmentStyle.description,
            "virtualInvestment" to mapOf(
                "summary" to investmentSummary.summary,
                "returnRate" to investmentSummary.returnRate
            )
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

        payload["question"] = request.question ?: defaultQuestion(mode)

        return ConsultingContextBundle(
            systemPersona = buildSystemPersona(mode),
            payload = payload
        )
    }

    private fun summarizeVirtualInvestment(userId: Long, stockCode: String): VirtualInvestmentSnapshot {
        val positions = virtualInvestmentService.getUserVirtualInvestments(userId, holdingOnly = true)
            .filter { it.stockCode.equals(stockCode, ignoreCase = true) }

        if (positions.isEmpty()) {
            return VirtualInvestmentSnapshot(
                returnRate = null,
                summary = "현재 보유 중인 해당 모의투자 종목이 없어 비교 수익률은 없음"
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
            totalProfit.multiply(HUNDRED).divide(totalBuyAmount, 2, RoundingMode.HALF_UP)
        }

        return VirtualInvestmentSnapshot(
            returnRate = returnRate,
            summary = "현재 평단가 대비 수익률 ${returnRate.toPlainString()}%"
        )
    }

    private fun buildSystemPersona(mode: ConsultingMode): String {
        val base = StringBuilder()
            .append("너는 증시 데이터 기반으로 투자 판단을 돕는 한국어 상담 AI야. ")
            .append("사용자가 제공한 JSON만 근거로 답변하고, 과장 없이 4~6문장으로 말해. ")
            .append("고양이 집사 컨셉을 유지하되 분석은 냉정하게 해. ")

        if (mode.includesSaju()) {
            base.append("사주가 포함되면 일간, 월지, 십성, 대운/세운을 투자 해석에 반영해. ")
        }
        if (mode.includesTarot()) {
            base.append("타로가 포함되면 카드의 상징과 직관적 메시지를 투자 심리와 타이밍 보조 지표로 활용해. ")
        }

        base.append(
            when (mode) {
                ConsultingMode.MARKET_ONLY ->
                    "증시 데이터와 투자 성향, 현재 수익률만으로 매매 관점과 리스크 관리 포인트를 정리해."

                ConsultingMode.MARKET_SAJU ->
                    "증시 데이터와 사주를 결합해 시장 적합성, 투자 스타일, 비중 확대/축소 타이밍을 조언해."

                ConsultingMode.MARKET_TAROT ->
                    "증시 데이터와 타로를 결합해 현재 심리 흐름, 진입/관망 판단, 리스크 신호를 조언해."

                ConsultingMode.MARKET_SAJU_TAROT ->
                    "증시 데이터, 사주, 타로를 함께 보고 공통 신호와 충돌 신호를 구분해서 조언해."
            }
        )

        return base.toString()
    }

    private fun defaultQuestion(mode: ConsultingMode): String =
        when (mode) {
            ConsultingMode.MARKET_ONLY ->
                "증시 데이터와 현재 수익률만 보고 지금 매수 유지, 추가 매수, 차익 실현 중 무엇이 나은지 말해줘."

            ConsultingMode.MARKET_SAJU ->
                "증시와 사주를 같이 보고 지금 비중을 늘릴지 줄일지 말해줘."

            ConsultingMode.MARKET_TAROT ->
                "증시와 타로를 같이 보고 지금 진입이 맞는지 관망이 맞는지 말해줘."

            ConsultingMode.MARKET_SAJU_TAROT ->
                "증시, 사주, 타로를 함께 보고 지금 공격적으로 갈지 방어적으로 갈지 말해줘."
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
        val dominant = analysis.tenGods
            .groupingBy { it.tenGod }
            .eachCount()
            .maxByOrNull { it.value }
            ?.key
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

    private fun buildFortuneSummary(analysis: SajuAnalysisResult): String {
        val yearly = formatTenGod(analysis.annualFortune.stemTenGod)
        val major = formatTenGod(analysis.majorFortune.stemTenGod)
        return "세운은 $yearly, 대운은 $major 흐름"
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

    private fun validateRequest(request: ComparativeConsultingRequest) {
        if (request.modes.isEmpty()) {
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "at least one consulting mode is required")
        }
        if (request.modes.any { it.includesSaju() } && (request.birthDateTime == null || request.majorFortunePillar == null)) {
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "birthDateTime and majorFortunePillar are required for saju modes")
        }
        if (request.modes.any { it.includesTarot() } && request.tarotCard == null) {
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "tarotCard is required for tarot modes")
        }
    }

    private companion object {
        val HUNDRED: BigDecimal = BigDecimal("100")
    }
}

data class ComparativeConsultingRequest(
    val userId: Long,
    val userName: String,
    val stockCode: String,
    val investmentStyle: InvestmentStyle,
    val modes: Set<ConsultingMode> = ConsultingMode.entries.toSet(),
    val birthDateTime: LocalDateTime? = null,
    val majorFortunePillar: Pillar? = null,
    val tarotCard: TarotCard? = null,
    val question: String? = null,
    val referenceDateTime: LocalDateTime = LocalDateTime.now(ZoneId.of("Asia/Seoul")),
    val zoneId: ZoneId = ZoneId.of("Asia/Seoul")
) {
    init {
        require(stockCode.isNotBlank()) { "stockCode must not be blank" }
    }
}

data class ComparativeConsultingResponse(
    val stock: StockContext,
    val virtualInvestment: VirtualInvestmentSnapshot,
    val results: Map<ConsultingMode, ModeConsultingResult>
)

data class ModeConsultingResult(
    val mode: ConsultingMode,
    val systemPersona: String,
    val payload: Map<String, Any?>,
    val response: StockFortuneAdviceResponse
)

data class ConsultingContextBundle(
    val systemPersona: String,
    val payload: Map<String, Any?>
)

data class VirtualInvestmentSnapshot(
    val returnRate: BigDecimal?,
    val summary: String
)

enum class ConsultingMode {
    MARKET_ONLY,
    MARKET_SAJU,
    MARKET_TAROT,
    MARKET_SAJU_TAROT;

    fun includesSaju(): Boolean = this == MARKET_SAJU || this == MARKET_SAJU_TAROT

    fun includesTarot(): Boolean = this == MARKET_TAROT || this == MARKET_SAJU_TAROT
}

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
