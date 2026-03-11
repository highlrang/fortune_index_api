package com.hwcompany.fortune_index.consulting

import com.fasterxml.jackson.databind.ObjectMapper
import com.hwcompany.fortune_index.ai.AiChatRequest
import com.hwcompany.fortune_index.ai.StockFortuneAdviceResponse
import com.hwcompany.fortune_index.ai.StockFortuneAdviceService
import com.hwcompany.fortune_index.ai.FiveElementsInput
import com.hwcompany.fortune_index.market.StockInfo
import com.hwcompany.fortune_index.tarot.TarotCard
import org.springframework.stereotype.Service

@Service
class ConsultingService(
    private val stockFortuneAdviceService: StockFortuneAdviceService,
    private val objectMapper: ObjectMapper
) {
    fun requestConsulting(request: ConsultingRequest): StockFortuneAdviceResponse {
        val contextPayload = mapOf(
            "userName" to request.userName,
            "saju" to mapOf(
                "fiveElements" to request.fiveElements,
                "summary" to buildFiveElementsSummary(request.fiveElements)
            ),
            "tarot" to mapOf(
                "cardName" to request.tarot.card.displayName,
                "arcanaType" to request.tarot.card.arcanaType.name,
                "uprightMeaning" to request.tarot.card.uprightMeaning,
                "imageUrl" to request.tarot.card.imageUrl,
                "selectedIndex" to request.tarot.index
            ),
            "stock" to mapOf(
                "ticker" to request.stock.ticker,
                "currentPrice" to request.stock.currentPrice,
                "changeRate" to request.stock.changeRate,
                "sector" to request.stock.sector,
                "fallback" to request.stock.fallback,
                "summary" to buildStockSummary(request.stock)
            ),
            "question" to (request.question ?: DEFAULT_QUESTION)
        )

        return stockFortuneAdviceService.generateAdvice(
            AiChatRequest(
                systemPersona = CONSULTING_PERSONA,
                userMessage = objectMapper.writeValueAsString(contextPayload)
            )
        )
    }

    private fun buildFiveElementsSummary(fiveElements: FiveElementsInput): String {
        val orderedElements = listOf(
            "목" to fiveElements.wood,
            "화" to fiveElements.fire,
            "토" to fiveElements.earth,
            "금" to fiveElements.metal,
            "수" to fiveElements.water
        ).sortedByDescending { it.second }

        return orderedElements.joinToString(", ") { (name, value) ->
            "$name ${value.stripTrailingZeros().toPlainString()}"
        }
    }

    private fun buildStockSummary(stock: StockInfo): String {
        val direction = when {
            stock.changeRate.signum() > 0 -> "${stock.changeRate.stripTrailingZeros().toPlainString()}% 상승 중"
            stock.changeRate.signum() < 0 -> "${stock.changeRate.abs().stripTrailingZeros().toPlainString()}% 하락 중"
            else -> "보합권"
        }

        return "${stock.ticker}, $direction, 섹터는 ${stock.sector}"
    }

    private companion object {
        private const val DEFAULT_QUESTION =
            "사주 오행, 타로 카드, 주식 흐름을 함께 해석해서 지금 어떤 투자 태도가 맞는지 조언해줘."

        private const val CONSULTING_PERSONA =
            "너는 데이터 기반의 냉철한 분석과 사주/타로의 직관을 결합한 독보적인 투자 상담가야. " +
                "주식의 지표를 기본으로 하되, 사용자의 운기가 이 시장 흐름과 어떻게 맞물리는지 위트 있게 조언해줘. " +
                "말투는 고양이가 말하는 것처럼 친근하지만 전문적이어야 해. " +
                "답변은 한국어로 4~6문장, 핵심 해석과 한 줄 행동 제안을 포함해."
    }
}

data class ConsultingRequest(
    val userName: String,
    val fiveElements: FiveElementsInput,
    val tarot: ConsultingTarotCard,
    val stock: StockInfo,
    val question: String? = null
)

data class ConsultingTarotCard(
    val index: Int,
    val card: TarotCard
)
