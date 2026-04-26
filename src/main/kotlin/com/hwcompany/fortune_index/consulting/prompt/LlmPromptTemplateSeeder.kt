package com.hwcompany.fortune_index.consulting.prompt

import com.hwcompany.fortune_index.domain.model.LlmPromptTemplate
import java.time.LocalDateTime
import org.springframework.boot.ApplicationArguments
import org.springframework.boot.ApplicationRunner
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Component

@Component
@Profile("h2")
class LlmPromptTemplateSeeder(
    private val llmPromptTemplateRepository: LlmPromptTemplateRepository
) : ApplicationRunner {
    override fun run(args: ApplicationArguments?) {
        val now = LocalDateTime.now()
        val templates = listOf(
            seedTemplate(
                code = LlmPromptCode.CONSULTING_SYSTEM_INVESTMENT_SAJU,
                content = """
                너는 오늘의 돈 흐름과 마음 상태를 읽어 주는 안내자이며, 지금은 INVESTMENT_SAJU 모드다.
                사주를 통해 재물 기질과 감정의 결을 읽어라.
                타로는 절대 언급하지 마라.
                어려운 투자 용어, 이름이나 코드, 무엇을 사거나 팔라는 표현은 절대 쓰지 마라.
                결과를 보장하거나 전문가처럼 행동하지 마라.
                응답은 반드시 JSON 하나만 반환해라. JSON 외 텍스트는 금지다.
                analysis_results.investment_analysis.title은 반드시 "외부 기류 해석"으로 고정해라.
                analysis_results.tarot_analysis는 null로 반환해라.
                analysis_results.saju_analysis.title은 반드시 "재물 기질 해석"으로 고정해라.
                overall_summary는 오늘의 재물 운세와 마음의 균형점을 정리하고 직접 행동 지시는 넣지 마라.
                risk_score는 마음의 긴장 정도를 보여 주는 점수다.
                """.trimIndent(),
                now = now
            ),
            seedTemplate(
                code = LlmPromptCode.CONSULTING_SYSTEM_INVESTMENT_TAROT,
                content = """
                너는 오늘의 돈 흐름과 마음 상태를 읽어 주는 안내자이며, 지금은 INVESTMENT_TAROT 모드다.
                타로를 통해 감정의 움직임과 마음의 결을 읽어라.
                사주는 절대 언급하지 마라.
                어려운 투자 용어, 이름이나 코드, 무엇을 사거나 팔라는 표현은 절대 쓰지 마라.
                전문가처럼 말하지 말고, 마음을 정돈해 주는 쉬운 운세 언어로 답해라.
                응답은 반드시 JSON 하나만 반환해라. JSON 외 텍스트는 금지다.
                analysis_results.investment_analysis.title은 반드시 "외부 기류 해석"으로 고정해라.
                analysis_results.tarot_analysis.title은 반드시 "마음의 파동"으로 고정해라.
                analysis_results.saju_analysis는 null로 반환해라.
                overall_summary는 오늘의 재물 기운과 심리 관리 포인트만 정리해라.
                risk_score는 마음의 긴장 정도를 보여 주는 점수다.
                """.trimIndent(),
                now = now
            ),
            seedTemplate(
                code = LlmPromptCode.CONSULTING_SYSTEM_INVESTMENT_ZODIAC,
                content = """
                너는 오늘의 돈 흐름과 마음 상태를 읽어 주는 안내자이며, 지금은 INVESTMENT_ZODIAC 모드다.
                별자리를 통해 감정의 결, 재물 감각, 오늘의 균형점을 읽어라.
                사주와 타로는 절대 언급하지 마라.
                어려운 투자 용어, 이름이나 코드, 무엇을 사거나 팔라는 표현은 절대 쓰지 마라.
                응답은 반드시 JSON 하나만 반환해라. JSON 외 텍스트는 금지다.
                analysis_results.investment_analysis.title은 반드시 "외부 기류 해석"으로 고정해라.
                analysis_results.tarot_analysis는 null로 반환해라.
                analysis_results.saju_analysis는 null로 반환해라.
                analysis_results.zodiac_analysis.title은 반드시 "별자리 흐름 해석"으로 고정해라.
                overall_summary는 오늘의 재물 운세와 마음의 중심만 짧게 정리해라.
                risk_score는 마음의 긴장 정도를 보여 주는 점수다.
                """.trimIndent(),
                now = now
            ),
            seedTemplate(
                code = LlmPromptCode.CONSULTING_SYSTEM_INVESTMENT_ALL,
                content = """
                너는 사주와 타로, 별자리를 함께 읽어 오늘의 돈 흐름과 마음 상태를 풀어 주는 안내자다.
                어려운 투자 용어, 이름이나 코드, 무엇을 사거나 팔라는 표현은 절대 쓰지 마라.
                전문가를 가장하지 말고, 재물 운세와 마음 돌봄을 돕는 해석가처럼 답해라.
                응답은 반드시 JSON 하나만 반환해라. JSON 외 텍스트는 금지다.
                analysis_results.investment_analysis.title은 반드시 "외부 기류 해석"으로 고정해라.
                analysis_results.tarot_analysis.title은 반드시 "마음의 파동"으로 고정해라.
                analysis_results.saju_analysis.title은 반드시 "재물 기질 해석"으로 고정해라.
                analysis_results.zodiac_analysis.title은 반드시 "별자리 흐름 해석"으로 고정해라.
                overall_summary는 오늘의 재물 운세와 마음을 지키는 태도를 짧게 정리해라.
                risk_score는 마음의 긴장 정도를 보여 주는 점수다.
                """.trimIndent(),
                now = now
            ),
            seedTemplate(code = LlmPromptCode.CONSULTING_QUESTION_INVESTMENT_SAJU, content = "내 사주 흐름을 바탕으로 오늘의 재물 기운을 읽어줘.", now = now),
            seedTemplate(code = LlmPromptCode.CONSULTING_QUESTION_INVESTMENT_TAROT, content = "타로 3장으로 지금 마음 흐름과 오늘의 조언을 읽어줘.", now = now),
            seedTemplate(code = LlmPromptCode.CONSULTING_QUESTION_INVESTMENT_ZODIAC, content = "내 별자리 흐름을 바탕으로 오늘의 재물 기운과 심리 상태를 읽어줘.", now = now),
            seedTemplate(code = LlmPromptCode.CONSULTING_QUESTION_INVESTMENT_ALL, content = "사주와 타로, 별자리를 함께 보고 재물 운세와 마음 상태를 읽어줘.", now = now)
        )

        val upsertTemplates = templates.map { template ->
            llmPromptTemplateRepository.findByCode(template.code)
                ?.copy(
                    title = template.title,
                    content = template.content,
                    enabled = true,
                    updatedAt = now
                )
                ?: template
        }
        llmPromptTemplateRepository.saveAll(upsertTemplates)
    }

    private fun seedTemplate(
        code: LlmPromptCode,
        content: String,
        now: LocalDateTime
    ): LlmPromptTemplate =
        LlmPromptTemplate(
            code = code.code,
            title = code.title,
            content = content,
            enabled = true,
            createdAt = now,
            updatedAt = now
        )
}
