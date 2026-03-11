package com.hwcompany.fortune_index.ai

import java.math.BigDecimal

data class StockFortuneAdviceRequest(
    val userName: String,
    val fiveElements: FiveElementsInput,
    val sectorChanges: List<SectorChangeInput>,
    val question: String? = null
)

data class AiChatRequest(
    val systemPersona: String,
    val userMessage: String
)

data class FiveElementsInput(
    val wood: BigDecimal,
    val fire: BigDecimal,
    val earth: BigDecimal,
    val metal: BigDecimal,
    val water: BigDecimal
)

data class SectorChangeInput(
    val sector: String,
    val changeRate: BigDecimal
)

data class StockFortuneAdviceResponse(
    val provider: AiProvider,
    val model: String,
    val content: String
)
