package com.hwcompany.fortune_index.ai

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import org.junit.jupiter.api.Test
import org.springframework.web.client.RestClient

class HybridConsultingAiClientTest {
    private val objectMapper: ObjectMapper = jacksonObjectMapper()

    @Test
    fun `parseJsonContent falls back to requested mode when response omits mode`() {
        val client = HybridConsultingAiClient(
            restClientBuilder = RestClient.builder(),
            properties = AiAdviceProperties().apply {
                provider = AiProvider.GEMINI
                gemini = GeminiProperties(
                    apiKey = "test-key",
                    model = "test-model"
                )
            },
            objectMapper = objectMapper
        )

        val method = HybridConsultingAiClient::class.java.getDeclaredMethod(
            "parseJsonContent",
            String::class.java,
            AiProvider::class.java,
            String::class.java,
            String::class.java,
            Class.forName("com.hwcompany.fortune_index.ai.GeminiGroundingMetadata")
        ).apply { isAccessible = true }

        val response = method.invoke(
            client,
            """
            {
              "analysis_results": {
                "market_analysis": {"title": "증시 관련 분석", "content": "시장 분석"},
                "tarot_analysis": {"title": "타로 카드 분석", "content": "타로 분석"},
                "saju_analysis": {"title": "사주 분석", "content": "사주 분석"}
              },
              "overall_summary": "종합 요약",
              "risk_score": 37
            }
            """.trimIndent(),
            AiProvider.GEMINI,
            "test-model",
            "STOCK_ALL",
            null
        ) as HybridConsultingAiResponse

        assertEquals("STOCK_ALL", response.mode)
        assertEquals("종합 요약", response.finalAdvice)
        assertEquals(37, response.riskScore)
        assertNotNull(response.analysisResults)
        assertEquals(false, response.evidence.grounded)
    }
}
