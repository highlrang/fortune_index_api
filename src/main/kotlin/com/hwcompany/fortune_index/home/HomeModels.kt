package com.hwcompany.fortune_index.home

import com.hwcompany.fortune_index.tarot.TarotDrawResult
import java.time.LocalDate
import java.time.LocalDateTime

data class HomeSummaryResponse(
    val summary: String,
    val saju: HomeCardSnapshot,
    val tarot: HomeCardSnapshot,
    val zodiac: HomeCardSnapshot
)

data class HomeCardSnapshot(
    val name: String,
    val summary: String
)

data class HomeDailyTarotDrawResponse(
    val drawDate: LocalDate,
    val drawn: Boolean,
    val canDraw: Boolean,
    val drawnAt: LocalDateTime?,
    val deckVersionId: String?,
    val cards: List<TarotDrawResult>
)
