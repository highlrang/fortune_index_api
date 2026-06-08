package com.hwcompany.fortune_index.admin

import com.fasterxml.jackson.databind.JsonNode
import com.hwcompany.fortune_index.consulting.ConsultRequest
import com.hwcompany.fortune_index.consulting.ConsultingService
import com.hwcompany.fortune_index.consulting.prompt.LlmPromptCode
import com.hwcompany.fortune_index.consulting.prompt.LlmPromptTemplateRepository
import com.hwcompany.fortune_index.consulting.prompt.LlmPromptTemplateService
import com.hwcompany.fortune_index.common.SeoulTime
import com.hwcompany.fortune_index.domain.model.LlmPromptTemplate
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size
import java.time.LocalDateTime
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.server.ResponseStatusException

@Service
class AdminPromptService(
    private val llmPromptTemplateRepository: LlmPromptTemplateRepository,
    private val llmPromptTemplateService: LlmPromptTemplateService,
    private val consultingService: ConsultingService
) {
    @Transactional(readOnly = true)
    fun listTemplates(): List<AdminPromptTemplateResponse> {
        val promptCodes = LlmPromptCode.entries.map { it.code }
        val storedByCode = llmPromptTemplateRepository.findAllByCodeIn(promptCodes)
            .associateBy { it.code }

        return LlmPromptCode.entries.map { promptCode ->
            val stored = storedByCode[promptCode.code]
            AdminPromptTemplateResponse.from(
                code = promptCode,
                stored = stored,
                defaultContent = llmPromptTemplateService.getDefaultContent(promptCode)
            )
        }
    }

    @Transactional(readOnly = true)
    fun getTemplate(code: String): AdminPromptTemplateResponse {
        val promptCode = resolvePromptCode(code)
        return AdminPromptTemplateResponse.from(
            code = promptCode,
            stored = llmPromptTemplateRepository.findByCode(promptCode.code),
            defaultContent = llmPromptTemplateService.getDefaultContent(promptCode)
        )
    }

    @Transactional
    fun updateTemplate(code: String, request: AdminPromptTemplateUpdateRequest): AdminPromptTemplateResponse {
        val promptCode = resolvePromptCode(code)
        val now = SeoulTime.now()
        val title = request.title?.trim()?.takeIf { it.isNotBlank() } ?: promptCode.title
        val content = request.content.trim()
        if (content.isBlank()) {
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "content must not be blank")
        }

        val saved = llmPromptTemplateRepository.save(
            llmPromptTemplateRepository.findByCode(promptCode.code)
                ?.copy(
                    title = title,
                    content = content,
                    enabled = request.enabled,
                    updatedAt = now
                )
                ?: LlmPromptTemplate(
                    code = promptCode.code,
                    title = title,
                    content = content,
                    enabled = request.enabled,
                    createdAt = now,
                    updatedAt = now
                )
        )

        return AdminPromptTemplateResponse.from(
            code = promptCode,
            stored = saved,
            defaultContent = llmPromptTemplateService.getDefaultContent(promptCode)
        )
    }

    @Transactional
    fun resetTemplate(code: String): AdminPromptTemplateResponse {
        val promptCode = resolvePromptCode(code)
        val now = SeoulTime.now()
        val defaultContent = llmPromptTemplateService.getDefaultContent(promptCode)
        val saved = llmPromptTemplateRepository.save(
            llmPromptTemplateRepository.findByCode(promptCode.code)
                ?.copy(
                    title = promptCode.title,
                    content = defaultContent,
                    enabled = true,
                    updatedAt = now
                )
                ?: LlmPromptTemplate(
                    code = promptCode.code,
                    title = promptCode.title,
                    content = defaultContent,
                    enabled = true,
                    createdAt = now,
                    updatedAt = now
                )
        )

        return AdminPromptTemplateResponse.from(
            code = promptCode,
            stored = saved,
            defaultContent = defaultContent
        )
    }

    @Transactional
    fun disableTemplate(code: String): AdminPromptTemplateResponse {
        val promptCode = resolvePromptCode(code)
        val now = SeoulTime.now()
        val existing = llmPromptTemplateRepository.findByCode(promptCode.code)
            ?: throw ResponseStatusException(HttpStatus.NOT_FOUND, "prompt template is not stored in database: $code")
        val saved = llmPromptTemplateRepository.save(
            existing.copy(
                enabled = false,
                updatedAt = now
            )
        )

        return AdminPromptTemplateResponse.from(
            code = promptCode,
            stored = saved,
            defaultContent = llmPromptTemplateService.getDefaultContent(promptCode)
        )
    }

    @Transactional(readOnly = true)
    fun previewConsultingPrompt(request: ConsultRequest): AdminConsultingPromptPreviewResponse {
        val prepared = consultingService.prepareConsultation(request)
        return AdminConsultingPromptPreviewResponse(
            mode = request.mode.name,
            scenario = prepared.scenario.name,
            userId = prepared.userId,
            riskProfile = prepared.riskProfile.name,
            question = prepared.question,
            systemPrompt = prepared.prompt,
            systemPromptChars = prepared.prompt.length,
            payload = prepared.payload,
            payloadChars = prepared.payload.toString().length
        )
    }

    private fun resolvePromptCode(code: String): LlmPromptCode =
        LlmPromptCode.entries.firstOrNull { it.code == code }
            ?: throw ResponseStatusException(HttpStatus.NOT_FOUND, "unknown prompt code: $code")
}

data class AdminPromptTemplateUpdateRequest(
    @field:Size(max = 200)
    val title: String? = null,
    @field:NotBlank
    @field:Size(max = 2500)
    val content: String,
    val enabled: Boolean = true
)

data class AdminPromptTemplateResponse(
    val code: String,
    val title: String,
    val content: String,
    val enabled: Boolean,
    val source: PromptTemplateSource,
    val defaultContent: String,
    val createdAt: LocalDateTime?,
    val updatedAt: LocalDateTime?
) {
    companion object {
        fun from(
            code: LlmPromptCode,
            stored: LlmPromptTemplate?,
            defaultContent: String
        ): AdminPromptTemplateResponse =
            AdminPromptTemplateResponse(
                code = code.code,
                title = stored?.title ?: code.title,
                content = stored?.content ?: defaultContent,
                enabled = stored?.enabled ?: false,
                source = when {
                    stored == null -> PromptTemplateSource.DEFAULT_CODE
                    stored.enabled -> PromptTemplateSource.DATABASE
                    else -> PromptTemplateSource.DISABLED_DATABASE_FALLBACK_CODE
                },
                defaultContent = defaultContent,
                createdAt = stored?.createdAt,
                updatedAt = stored?.updatedAt
            )
    }
}

enum class PromptTemplateSource {
    DATABASE,
    DEFAULT_CODE,
    DISABLED_DATABASE_FALLBACK_CODE
}

data class AdminConsultingPromptPreviewResponse(
    val mode: String,
    val scenario: String,
    val userId: Long,
    val riskProfile: String,
    val question: String,
    val systemPrompt: String,
    val systemPromptChars: Int,
    val payload: JsonNode,
    val payloadChars: Int
)
