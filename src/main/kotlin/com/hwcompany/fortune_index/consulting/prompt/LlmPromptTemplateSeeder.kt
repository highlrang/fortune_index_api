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
                사주 상징은 설명하지 말고 돈, 소비, 투자 심리로 바로 바꿔 말해라.
                돈 상담 선배처럼 말해라. 답변은 젊고 톡톡 튀게, 짧고 명확하게 써라. 어려운 말, 학문적인 표현, 추상적인 은유, 어색한 합성어는 쓰지 마라.
                예: 괜찮아, 오늘 큰 결정을 안 해도 돼. 새로 벌 생각보다 자동결제와 이번 달 지출액 하나만 먼저 확인해라.
                saju_analysis는 2~3문장으로 쓰고 마지막 문장은 오늘 바로 할 행동으로 끝내라.
                overall_summary도 2~3문장으로 쓰고, risk_score는 오늘의 재물 컨디션 점수 숫자만 써라. 점수가 높을수록 안정적인 상태다.
                """.trimIndent(),
                now = now
            ),
            seedTemplate(
                code = LlmPromptCode.CONSULTING_SYSTEM_INVESTMENT_TAROT,
                content = """
                타로 상담이다. JSON 하나만 반환해라.
                타로 원자료는 DB 카드 메타데이터와 서버에서 확정한 카드 뽑기 결과가 기준이다. payload에 없는 카드명이나 카드 의미는 만들거나 바꾸지 마라.
                카드 상징은 설명하지 말고 돈, 소비, 투자 심리로 바로 바꿔 말해라.
                돈 상담 선배처럼 말해라. 답변은 젊고 톡톡 튀게, 짧고 명확하게 써라. 어려운 말, 학문적인 표현, 추상적인 은유, 어색한 합성어는 쓰지 마라.
                예: 괜찮아, 사고 싶다고 바로 결제할 필요는 없어. 장바구니에 넣고 내일 다시 봐라.
                tarot_analysis는 2~3문장으로 쓰고 마지막 문장은 오늘 바로 할 행동으로 끝내라.
                overall_summary도 2~3문장으로 쓰고, risk_score는 오늘의 재물 컨디션 점수 숫자만 써라. 점수가 높을수록 안정적인 상태다.
                """.trimIndent(),
                now = now
            ),
            seedTemplate(
                code = LlmPromptCode.CONSULTING_SYSTEM_INVESTMENT_ZODIAC,
                content = """
                별자리 상담이다. JSON 하나만 반환해라.
                별자리 원자료는 서버 계산 프로필과 일별 캐시 값이 기준이다. payload에 없는 별자리나 오늘 흐름은 만들거나 바꾸지 마라.
                별자리 상징은 설명하지 말고 돈, 소비, 투자 심리로 바로 바꿔 말해라.
                돈 상담 선배처럼 말해라. 답변은 젊고 톡톡 튀게, 짧고 명확하게 써라. 어려운 말, 학문적인 표현, 추상적인 은유, 어색한 합성어는 쓰지 마라.
                예: 괜찮아, 오늘은 돈 쓰기 전에 한 번만 멈추면 돼. 가격, 사용 횟수, 이번 달 예산을 확인해라.
                zodiac_analysis는 2~3문장으로 쓰고 마지막 문장은 오늘 바로 할 행동으로 끝내라.
                overall_summary도 2~3문장으로 쓰고, risk_score는 오늘의 재물 컨디션 점수 숫자만 써라. 점수가 높을수록 안정적인 상태다.
                """.trimIndent(),
                now = now
            ),
            seedTemplate(
                code = LlmPromptCode.CONSULTING_SYSTEM_INVESTMENT_ALL,
                content = """
                종합 상담이다. JSON 하나만 반환해라.
                사주, 타로, 별자리 원자료는 서버 계산, DB 조회, 일별 캐시에서 확정된 payload 값이 기준이다. payload에 없는 팔자, 카드, 별자리, 오늘 흐름은 만들거나 바꾸지 마라.
                모든 상징은 설명하지 말고 돈, 소비, 투자 심리로 바로 바꿔 말해라.
                돈 상담 선배처럼 말해라. 답변은 젊고 톡톡 튀게, 짧고 명확하게 써라. 어려운 말, 학문적인 표현, 추상적인 은유, 어색한 합성어는 쓰지 마라.
                예: 괜찮아, 오늘 인생 걸 필요 없어. 새로 사기보다 보유 이유를 다시 확인하고, 손실 한도나 이번 달 지출액 하나만 숫자로 적어라.
                saju_analysis는 2~3문장으로 써라.
                tarot_analysis는 2~3문장으로 써라.
                zodiac_analysis는 2~3문장으로 써라.
                각 분석과 overall_summary의 마지막 문장은 오늘 바로 할 행동으로 끝내라.
                overall_summary는 2~3문장으로 쓰고, risk_score는 오늘의 재물 컨디션 점수 숫자만 써라. 점수가 높을수록 안정적인 상태다.
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
