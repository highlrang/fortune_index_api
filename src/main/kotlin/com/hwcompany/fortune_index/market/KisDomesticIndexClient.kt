package com.hwcompany.fortune_index.market

import com.fasterxml.jackson.databind.JsonNode
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

@Component
class KisDomesticIndexClient(
    private val kisMarketInsightService: KisMarketInsightService
) {
    fun fetchSnapshot(indexCode: String): KisDomesticIndexSnapshot? {
        val response = kisMarketInsightService.getSectorIndex(SectorIndexLookupRequest(indexCode = indexCode))
        val payload = response.primaryOutputNode() ?: return null
        val currentValue = payload.decimal(
            "bstp_nmix_prpr",
            "bstp_cls_prpr",
            "stck_prpr",
            "cur_prc"
        ) ?: return null
        val change = payload.decimal(
            "bstp_prdy_vrss",
            "prdy_vrss",
            "cmppr"
        ) ?: java.math.BigDecimal.ZERO
        val changeRate = payload.decimal(
            "prdy_ctrt",
            "bstp_prdy_ctrt",
            "flu_rt",
            "rate"
        ) ?: java.math.BigDecimal.ZERO

        return KisDomesticIndexSnapshot(
            indexCode = indexCode,
            currentValue = currentValue.toDouble(),
            change = change.toDouble(),
            changeRate = changeRate.toDouble(),
            asOf = payload.resolveAsOf() ?: ZonedDateTime.now(SEOUL_ZONE_ID)
        )
    }

    private fun KisApiRawResponse.primaryOutputNode(): JsonNode? =
        sequenceOf(output1, output2, output)
            .filterNotNull()
            .mapNotNull { node ->
                when {
                    node.isArray -> node.firstOrNull()
                    node.isObject -> node
                    else -> null
                }
            }
            .firstOrNull()

    private fun JsonNode.firstOrNull(): JsonNode? =
        if (isArray && size() > 0) get(0) else null

    private fun JsonNode.decimal(vararg fields: String): java.math.BigDecimal? =
        fields.firstNotNullOfOrNull { field ->
            get(field)
                ?.asText()
                ?.trim()
                ?.replace(",", "")
                ?.takeIf { it.isNotEmpty() }
                ?.toBigDecimalOrNull()
        }

    private fun JsonNode.resolveAsOf(): ZonedDateTime? {
        val date = text(
            "bstp_bsop_date",
            "bsop_date",
            "stck_bsop_date"
        )?.takeIf { it.length == 8 }
            ?.let { LocalDate.parse(it, DATE_FORMATTER) }
            ?: return null
        val time = text(
            "bstp_nmix_prpr_hour",
            "bsop_hour",
            "aspr_acpt_hour",
            "hour_cls_code"
        )?.digitsOnly()
            ?.takeIf { it.length >= 4 }
            ?.let {
                val hhmmss = it.padEnd(6, '0').take(6)
                LocalTime.of(
                    hhmmss.substring(0, 2).toInt(),
                    hhmmss.substring(2, 4).toInt(),
                    hhmmss.substring(4, 6).toInt()
                )
            }
            ?: LocalTime.of(15, 30)

        return ZonedDateTime.of(LocalDateTime.of(date, time), SEOUL_ZONE_ID)
    }

    private fun JsonNode.text(vararg fields: String): String? =
        fields.firstNotNullOfOrNull { field ->
            get(field)?.asText()?.trim()?.takeIf { it.isNotEmpty() }
        }

    private fun String.digitsOnly(): String =
        filter(Char::isDigit)

    private companion object {
        private val logger = LoggerFactory.getLogger(KisDomesticIndexClient::class.java)
        private val SEOUL_ZONE_ID: ZoneId = ZoneId.of("Asia/Seoul")
        private val DATE_FORMATTER = java.time.format.DateTimeFormatter.BASIC_ISO_DATE
    }
}

data class KisDomesticIndexSnapshot(
    val indexCode: String,
    val currentValue: Double,
    val change: Double,
    val changeRate: Double,
    val asOf: ZonedDateTime
)
