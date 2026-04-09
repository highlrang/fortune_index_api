package com.hwcompany.fortune_index.investmentindex

import com.hwcompany.fortune_index.domain.model.EarthlyBranch
import com.hwcompany.fortune_index.domain.model.HeavenlyStem
import com.hwcompany.fortune_index.saju.GanzhiCalculator
import com.hwcompany.fortune_index.tarot.TarotArcanaType
import com.hwcompany.fortune_index.tarot.TarotCard
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime
import kotlin.math.roundToInt
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import org.springframework.stereotype.Service

@Service
class InvestmentIndexService(
    private val marketIndexQuoteClient: MarketIndexQuoteClient
) {
    fun getInvestmentIndex(now: ZonedDateTime = ZonedDateTime.now(SEOUL_ZONE_ID)): TotalIndexResponse {
        val nowInSeoul = now.withZoneSameInstant(SEOUL_ZONE_ID)
        val selectedMarket = resolveMarket(nowInSeoul)
        val marketRawValue = runCatching {
            marketIndexQuoteClient.getChangeRate(selectedMarket.ticker)
        }.getOrElse { ex ->
            logger.warn("Failed to fetch {} market data. Falling back to neutral change rate.", selectedMarket.name, ex)
            0.0
        }
        val marketScore = normalizeMarketScore(marketRawValue)
        val sajuDetail = calculateSajuScore(nowInSeoul.toLocalDate())
        val tarotDetail = calculateTarotScore(nowInSeoul.toLocalDate())
        val totalScore = (
            marketScore * MARKET_WEIGHT +
                sajuDetail.score * SAJU_WEIGHT +
                tarotDetail.score * TAROT_WEIGHT
            ).roundToInt()

        return TotalIndexResponse(
            totalScore = totalScore.coerceIn(MIN_SCORE, MAX_SCORE),
            detail = ScoreDetail(
                selectedMarket = selectedMarket.name,
                marketScore = marketScore,
                marketRawValue = marketRawValue,
                sajuScore = sajuDetail.score,
                dailyGanji = sajuDetail.dailyGanji,
                tarotScore = tarotDetail.score,
                tarotCardName = tarotDetail.cardName
            )
        )
    }

    internal fun resolveMarket(now: ZonedDateTime): SupportedMarket {
        val localTime = now.toLocalTime()
        val dayOfWeek = now.dayOfWeek

        if (isWeekday(dayOfWeek) && localTime in KOSPI_OPEN_TIME..KOSPI_CLOSE_TIME) {
            return SupportedMarket.KOSPI
        }
        if (isWeekday(dayOfWeek) && (localTime >= NASDAQ_OPEN_TIME || localTime < NASDAQ_CLOSE_TIME)) {
            return SupportedMarket.NASDAQ
        }

        return when {
            isWeekday(dayOfWeek) && localTime < KOSPI_OPEN_TIME -> SupportedMarket.NASDAQ
            isWeekday(dayOfWeek) -> SupportedMarket.KOSPI
            else -> SupportedMarket.NASDAQ
        }
    }

    internal fun normalizeMarketScore(changeRate: Double): Int {
        val bounded = changeRate.coerceIn(-MARKET_SCORE_BAND, MARKET_SCORE_BAND)
        val normalized = ((bounded + MARKET_SCORE_BAND) / (MARKET_SCORE_BAND * 2)) * MAX_SCORE
        return normalized.roundToInt().coerceIn(MIN_SCORE, MAX_SCORE)
    }

    private fun calculateSajuScore(date: LocalDate): SajuIndexDetail {
        val dayPillar = GanzhiCalculator.calculate(date.atStartOfDay(), SEOUL_ZONE_ID).day
        val stemScore = STEM_SCORES.getValue(dayPillar.heavenlyStem)
        val branchScore = BRANCH_SCORES.getValue(dayPillar.earthlyBranch)
        val score = (stemScore * 0.6 + branchScore * 0.4).roundToInt()

        return SajuIndexDetail(
            score = score.coerceIn(MIN_SCORE, MAX_SCORE),
            dailyGanji = STEM_LABELS.getValue(dayPillar.heavenlyStem) + BRANCH_LABELS.getValue(dayPillar.earthlyBranch)
        )
    }

    private fun calculateTarotScore(date: LocalDate): TarotIndexDetail {
        val numerologyNumber = reduceToMajorArcanaNumber(
            "$date".filter(Char::isDigit).sumOf { it.digitToInt() }
        )
        val card = MAJOR_ARCANA_BY_NUMBER.getValue(numerologyNumber)
        val score = TAROT_SCORES.getValue(card)

        return TarotIndexDetail(
            score = score,
            cardName = card.displayName
        )
    }

    private fun reduceToMajorArcanaNumber(value: Int): Int {
        var reduced = value
        while (reduced > 22) {
            reduced = reduced.toString().sumOf { it.digitToInt() }
        }
        return if (reduced == 22) 0 else reduced.coerceAtLeast(1)
    }

    private fun isWeekday(dayOfWeek: DayOfWeek): Boolean =
        dayOfWeek != DayOfWeek.SATURDAY && dayOfWeek != DayOfWeek.SUNDAY

    private data class SajuIndexDetail(
        val score: Int,
        val dailyGanji: String
    )

    private data class TarotIndexDetail(
        val score: Int,
        val cardName: String
    )

    companion object {
        private val logger = LoggerFactory.getLogger(InvestmentIndexService::class.java)
        private val SEOUL_ZONE_ID: ZoneId = ZoneId.of("Asia/Seoul")
        private val KOSPI_OPEN_TIME: LocalTime = LocalTime.of(9, 0)
        private val KOSPI_CLOSE_TIME: LocalTime = LocalTime.of(15, 30)
        private val NASDAQ_OPEN_TIME: LocalTime = LocalTime.of(22, 30)
        private val NASDAQ_CLOSE_TIME: LocalTime = LocalTime.of(5, 0)
        private const val MIN_SCORE = 0
        private const val MAX_SCORE = 100
        private const val MARKET_SCORE_BAND = 5.0
        private const val MARKET_WEIGHT = 0.70
        private const val SAJU_WEIGHT = 0.15
        private const val TAROT_WEIGHT = 0.15

        private val MAJOR_ARCANA_BY_NUMBER = TarotCard.entries
            .filter { it.arcanaType == TarotArcanaType.MAJOR }
            .associateBy { it.cardNumber }

        private val STEM_SCORES = mapOf(
            HeavenlyStem.GAP to 86,
            HeavenlyStem.EUL to 82,
            HeavenlyStem.BYEONG to 90,
            HeavenlyStem.JEONG to 84,
            HeavenlyStem.MU to 72,
            HeavenlyStem.GI to 68,
            HeavenlyStem.GYEONG to 80,
            HeavenlyStem.SIN to 76,
            HeavenlyStem.IM to 88,
            HeavenlyStem.GYE to 83
        )

        private val BRANCH_SCORES = mapOf(
            EarthlyBranch.JA to 86,
            EarthlyBranch.CHUK to 70,
            EarthlyBranch.IN to 84,
            EarthlyBranch.MYO to 80,
            EarthlyBranch.JIN to 74,
            EarthlyBranch.SA to 88,
            EarthlyBranch.O to 90,
            EarthlyBranch.MI to 72,
            EarthlyBranch.SIN to 78,
            EarthlyBranch.YU to 76,
            EarthlyBranch.SUL to 70,
            EarthlyBranch.HAE to 84
        )

        private val STEM_LABELS = mapOf(
            HeavenlyStem.GAP to "갑",
            HeavenlyStem.EUL to "을",
            HeavenlyStem.BYEONG to "병",
            HeavenlyStem.JEONG to "정",
            HeavenlyStem.MU to "무",
            HeavenlyStem.GI to "기",
            HeavenlyStem.GYEONG to "경",
            HeavenlyStem.SIN to "신",
            HeavenlyStem.IM to "임",
            HeavenlyStem.GYE to "계"
        )

        private val BRANCH_LABELS = mapOf(
            EarthlyBranch.JA to "자",
            EarthlyBranch.CHUK to "축",
            EarthlyBranch.IN to "인",
            EarthlyBranch.MYO to "묘",
            EarthlyBranch.JIN to "진",
            EarthlyBranch.SA to "사",
            EarthlyBranch.O to "오",
            EarthlyBranch.MI to "미",
            EarthlyBranch.SIN to "신",
            EarthlyBranch.YU to "유",
            EarthlyBranch.SUL to "술",
            EarthlyBranch.HAE to "해"
        )

        private val TAROT_SCORES = mapOf(
            TarotCard.THE_FOOL to 78,
            TarotCard.THE_MAGICIAN to 90,
            TarotCard.THE_HIGH_PRIESTESS to 82,
            TarotCard.THE_EMPRESS to 88,
            TarotCard.THE_EMPEROR to 80,
            TarotCard.THE_HIEROPHANT to 74,
            TarotCard.THE_LOVERS to 86,
            TarotCard.THE_CHARIOT to 89,
            TarotCard.STRENGTH to 87,
            TarotCard.THE_HERMIT to 68,
            TarotCard.WHEEL_OF_FORTUNE to 91,
            TarotCard.JUSTICE to 77,
            TarotCard.THE_HANGED_MAN to 60,
            TarotCard.DEATH to 58,
            TarotCard.TEMPERANCE to 85,
            TarotCard.THE_DEVIL to 42,
            TarotCard.THE_TOWER to 35,
            TarotCard.THE_STAR to 92,
            TarotCard.THE_MOON to 52,
            TarotCard.THE_SUN to 95,
            TarotCard.JUDGEMENT to 79,
            TarotCard.THE_WORLD to 94
        )
    }
}

enum class SupportedMarket(
    val ticker: String
) {
    KOSPI("^KS11"),
    NASDAQ("^IXIC")
}

interface MarketIndexQuoteClient {
    fun getChangeRate(ticker: String): Double
}

@Component
class LocalMarketIndexQuoteClient : MarketIndexQuoteClient {
    override fun getChangeRate(ticker: String): Double = 0.0
}
