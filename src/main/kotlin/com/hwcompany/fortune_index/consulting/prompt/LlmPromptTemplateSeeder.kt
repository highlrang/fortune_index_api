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
                사주 상담이다. JSON 하나만 반환해라.
                사주 원자료는 서버 계산과 DB 저장값이 기준이다. payload에 없는 팔자, 대운, 세운은 만들거나 바꾸지 마라.
                saju_analysis는 3문장 안팎으로 써라.
                overall_summary는 3문장 안팎으로 쓰고, risk_score는 숫자만 써라.
                """.trimIndent(),
                now = now
            ),
            seedTemplate(
                code = LlmPromptCode.CONSULTING_SYSTEM_INVESTMENT_TAROT,
                content = """
                타로 상담이다. JSON 하나만 반환해라.
                타로 원자료는 DB 카드 메타데이터와 서버에서 확정한 카드 뽑기 결과가 기준이다. payload에 없는 카드명이나 카드 의미는 만들거나 바꾸지 마라.
                tarot_analysis는 3문장 안팎으로 써라.
                overall_summary는 3문장 안팎으로 쓰고, risk_score는 숫자만 써라.
                """.trimIndent(),
                now = now
            ),
            seedTemplate(
                code = LlmPromptCode.CONSULTING_SYSTEM_INVESTMENT_ZODIAC,
                content = """
                별자리 상담이다. JSON 하나만 반환해라.
                별자리 원자료는 서버 계산 프로필과 일별 캐시 값이 기준이다. payload에 없는 별자리나 오늘 흐름은 만들거나 바꾸지 마라.
                zodiac_analysis는 3문장 안팎으로 써라.
                overall_summary는 3문장 안팎으로 쓰고, risk_score는 숫자만 써라.
                """.trimIndent(),
                now = now
            ),
            seedTemplate(
                code = LlmPromptCode.CONSULTING_SYSTEM_INVESTMENT_ALL,
                content = """
                종합 상담이다. JSON 하나만 반환해라.
                사주, 타로, 별자리 원자료는 서버 계산, DB 조회, 일별 캐시에서 확정된 payload 값이 기준이다. payload에 없는 팔자, 카드, 별자리, 오늘 흐름은 만들거나 바꾸지 마라.
                saju_analysis는 3문장 안팎으로 써라.
                tarot_analysis는 3문장 안팎으로 써라.
                zodiac_analysis는 3문장 안팎으로 써라.
                overall_summary는 3문장 안팎으로 쓰고, risk_score는 숫자만 써라.
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
