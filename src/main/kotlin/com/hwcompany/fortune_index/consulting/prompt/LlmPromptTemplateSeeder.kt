package com.hwcompany.fortune_index.consulting.prompt

import com.hwcompany.fortune_index.common.SeoulTime
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
        val now = SeoulTime.now()
        val templates = listOf(
            seedTemplate(
                code = LlmPromptCode.CONSULTING_SYSTEM_INVESTMENT_SAJU,
                content = "사주 상담이다. payload.signals.saju의 개인 명식과 운 흐름을 주근거로 JSON만 반환해라.",
                now = now
            ),
            seedTemplate(
                code = LlmPromptCode.CONSULTING_SYSTEM_INVESTMENT_TAROT,
                content = "타로 상담이다. payload.signals.tarot.drawnCards 3장을 메인, birthTarotCard를 서브로 JSON만 반환해라.",
                now = now
            ),
            seedTemplate(
                code = LlmPromptCode.CONSULTING_SYSTEM_INVESTMENT_ZODIAC,
                content = "별자리 상담이다. payload.signals.zodiac의 element, moodKeyword, consultingAngle을 질문에 연결해 JSON만 반환해라.",
                now = now
            ),
            seedTemplate(
                code = LlmPromptCode.CONSULTING_SYSTEM_INVESTMENT_ALL,
                content = "종합 상담이다. 사주, 타로, 별자리 역할을 분리해 JSON만 반환해라.",
                now = now
            ),
            seedTemplate(code = LlmPromptCode.CONSULTING_QUESTION_INVESTMENT_SAJU, content = "사주 기준으로 오늘 투자 판단을 짧게 봐줘.", now = now),
            seedTemplate(code = LlmPromptCode.CONSULTING_QUESTION_INVESTMENT_TAROT, content = "타로 기준으로 오늘 투자 판단을 짧게 봐줘.", now = now),
            seedTemplate(code = LlmPromptCode.CONSULTING_QUESTION_INVESTMENT_ZODIAC, content = "별자리 기준으로 오늘 투자 판단을 짧게 봐줘.", now = now),
            seedTemplate(code = LlmPromptCode.CONSULTING_QUESTION_INVESTMENT_ALL, content = "사주, 타로, 별자리 기준으로 오늘 투자 판단을 짧게 봐줘.", now = now)
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
