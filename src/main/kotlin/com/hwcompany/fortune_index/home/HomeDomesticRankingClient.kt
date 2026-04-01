package com.hwcompany.fortune_index.home

import com.fasterxml.jackson.databind.JsonNode
import com.hwcompany.fortune_index.market.KisMarketInsightService
import com.hwcompany.fortune_index.market.VolumeRankLookupRequest
import java.math.BigDecimal
import org.springframework.stereotype.Component

@Component
class HomeDomesticRankingClient(
    private val kisMarketInsightService: KisMarketInsightService
) {
    fun fetchTopDomesticStocks(limit: Int): List<HomeDomesticRankedStock> {
        val response = kisMarketInsightService.getVolumeRank(
            VolumeRankLookupRequest(
                marketDivisionCode = "J",
                screenDivisionCode = "20171",
                inputIscd = "0000"
            )
        )

        val rows = response.output.asArrayItems()
            .mapNotNull { node -> node.toDomesticRankedStock() }

        return rows.take(limit)
    }

    private fun JsonNode.toDomesticRankedStock(): HomeDomesticRankedStock? {
        val ticker = text(
            "mksc_shrn_iscd",
            "stck_shrn_iscd",
            "pdno"
        ) ?: return null

        return HomeDomesticRankedStock(
            ticker = ticker,
            name = text(
                "hts_kor_isnm",
                "data_rank_name",
                "prdt_abrv_name"
            ) ?: ticker,
            price = decimal(
                "stck_prpr",
                "data_value",
                "cur_prc"
            ) ?: BigDecimal.ZERO,
            changeRate = decimal(
                "prdy_ctrt",
                "flu_rt",
                "rate"
            ) ?: BigDecimal.ZERO,
            transactionAmount = decimal(
                "acml_tr_pbmn",
                "acml_tr_pbmn_day",
                "trade_amt"
            ),
            volume = long(
                "acml_vol",
                "cntg_vol",
                "trade_vol"
            )
        )
    }

    private fun JsonNode?.asArrayItems(): List<JsonNode> =
        when {
            this == null -> emptyList()
            isArray -> (0 until size()).mapNotNull { get(it) }
            else -> emptyList()
        }

    private fun JsonNode.text(vararg fields: String): String? =
        fields.firstNotNullOfOrNull { field ->
            get(field)?.asText()?.trim()?.takeIf { it.isNotEmpty() }
        }

    private fun JsonNode.decimal(vararg fields: String): BigDecimal? =
        fields.firstNotNullOfOrNull { field ->
            get(field)?.asText()?.normalizeNumeric()?.toBigDecimalOrNull()
        }

    private fun JsonNode.long(vararg fields: String): Long? =
        fields.firstNotNullOfOrNull { field ->
            get(field)?.asText()?.normalizeNumeric()?.toLongOrNull()
        }

    private fun String.normalizeNumeric(): String? =
        trim().replace(",", "").takeIf { it.isNotEmpty() }
}

data class HomeDomesticRankedStock(
    val ticker: String,
    val name: String,
    val price: BigDecimal,
    val changeRate: BigDecimal,
    val transactionAmount: BigDecimal?,
    val volume: Long?
)
