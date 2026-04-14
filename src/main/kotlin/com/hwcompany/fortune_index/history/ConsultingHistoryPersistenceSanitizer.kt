package com.hwcompany.fortune_index.history

internal object ConsultingHistoryPersistenceSanitizer {
    private const val FOCUS_LABEL_MAX_LENGTH = 100
    private const val TICKER_MAX_LENGTH = 20
    private const val SAJU_SUMMARY_MAX_LENGTH = 500
    private const val TAROT_SUMMARY_MAX_LENGTH = 300
    private const val RETRO_NOTE_MAX_LENGTH = 1000

    fun focusLabel(value: String): String = value.trim().truncate(FOCUS_LABEL_MAX_LENGTH)

    fun ticker(value: String): String = value.trim().truncate(TICKER_MAX_LENGTH)

    fun sajuSummary(value: String): String = value.trim().truncate(SAJU_SUMMARY_MAX_LENGTH)

    fun tarotSummary(value: String): String = value.trim().truncate(TAROT_SUMMARY_MAX_LENGTH)

    fun nullableText(value: String?): String? =
        value?.trim()?.takeIf { it.isNotEmpty() }?.truncate(RETRO_NOTE_MAX_LENGTH)

    private fun String.truncate(maxLength: Int): String =
        if (length <= maxLength) this else take(maxLength)
}
