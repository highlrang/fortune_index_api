package com.hwcompany.fortune_index.home

import com.fasterxml.jackson.databind.JsonNode
import com.hwcompany.fortune_index.market.KisMarketInsightService
import com.hwcompany.fortune_index.market.KisOverseasMarket
import com.hwcompany.fortune_index.market.KisOverseasPriceClient
import com.hwcompany.fortune_index.market.OverseasRankLookupRequest
import java.math.BigDecimal
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

@Component
class HomeOverseasRankingClient(
    private val kisMarketInsightService: KisMarketInsightService,
    private val kisOverseasPriceClient: KisOverseasPriceClient
) {
    fun fetchTopOverseasStocks(limit: Int, market: KisOverseasMarket = DEFAULT_MARKET): List<HomeOverseasRankedStock> {
        val response = kisMarketInsightService.getOverseasRank(
            OverseasRankLookupRequest(
                exchangeCode = market.exchangeCode,
                rankingTypeCode = VOLUME_RANK_CODE
            )
        )

        return response.output.asArrayItems()
            .mapNotNull { node -> node.toOverseasRankSeed() }
            .take(limit)
            .mapNotNull { seed -> fetchRankedStock(seed, market) }
    }

    private fun fetchRankedStock(seed: OverseasRankSeed, market: KisOverseasMarket): HomeOverseasRankedStock? =
        runCatching { kisOverseasPriceClient.fetchSnapshot(seed.ticker, market) }
            .onFailure { ex ->
                logger.warn("Failed to fetch overseas ranked stock snapshot. ticker={}, market={}", seed.ticker, market.name, ex)
            }
            .getOrNull()
            ?.let { snapshot ->
                HomeOverseasRankedStock(
                    ticker = snapshot.ticker,
                    name = snapshot.name.ifBlank { seed.name ?: seed.ticker },
                    price = snapshot.price,
                    changeRate = snapshot.changeRate,
                    currency = snapshot.currency
                )
            }

    private fun JsonNode.toOverseasRankSeed(): OverseasRankSeed? {
        val ticker = text(
            "ovrs_pdno",
            "symbol",
            "symb",
            "rsym",
            "pdno"
        ) ?: return null

        return OverseasRankSeed(
            ticker = ticker,
            name = text(
                "ovrs_item_name",
                "prdt_name",
                "data_rank_name",
                "hts_kor_isnm",
                "prdt_abrv_name"
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

    private companion object {
        private val logger = LoggerFactory.getLogger(HomeOverseasRankingClient::class.java)
        private val DEFAULT_MARKET = KisOverseasMarket.NASDAQ
        private const val VOLUME_RANK_CODE = "0"
    }
}

data class HomeOverseasRankedStock(
    val ticker: String,
    val name: String,
    val price: BigDecimal,
    val changeRate: BigDecimal,
    val currency: String
)

private data class OverseasRankSeed(
    val ticker: String,
    val name: String?
)
