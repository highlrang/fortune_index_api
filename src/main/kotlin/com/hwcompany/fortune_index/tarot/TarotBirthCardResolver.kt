package com.hwcompany.fortune_index.tarot

fun resolveBirthTarotCard(dateDigits: String): TarotCard {
    val normalized = dateDigits.filter(Char::isDigit)
    require(normalized.isNotEmpty()) { "birth date digits must not be empty" }

    var reduced = normalized.sumOf { it.digitToInt() }
    while (reduced > 22) {
        reduced = reduced.toString().sumOf { it.digitToInt() }
    }
    val cardNumber = if (reduced == 22) 0 else reduced.coerceAtLeast(1)
    return MAJOR_ARCANA_BY_BIRTH_NUMBER.getValue(cardNumber)
}

private val MAJOR_ARCANA_BY_BIRTH_NUMBER = TarotCard.entries
    .filter { it.arcanaType == TarotArcanaType.MAJOR }
    .associateBy { it.cardNumber }
