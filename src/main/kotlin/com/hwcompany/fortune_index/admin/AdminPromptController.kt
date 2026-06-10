package com.hwcompany.fortune_index.admin

import com.hwcompany.fortune_index.auth.requireAdmin
import com.hwcompany.fortune_index.consulting.ConsultRequest
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.security.core.Authentication
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/admin/llm-prompts")
@Tag(name = "관리자 LLM 프롬프트 API", description = "LLM 프롬프트 템플릿 조회, 수정, 미리보기")
class AdminPromptController(
    private val adminPromptService: AdminPromptService,
    private val adminProperties: AdminProperties
) {
    @Operation(summary = "LLM 프롬프트 템플릿 목록 조회")
    @GetMapping
    fun listTemplates(authentication: Authentication): List<AdminPromptTemplateResponse> {
        authentication.requireAdmin(adminProperties)
        return adminPromptService.listTemplates()
    }

    @Operation(summary = "LLM 프롬프트 템플릿 단건 조회")
    @GetMapping("/{code}")
    fun getTemplate(
        authentication: Authentication,
        @PathVariable code: String
    ): AdminPromptTemplateResponse {
        authentication.requireAdmin(adminProperties)
        return adminPromptService.getTemplate(code)
    }

    @Operation(summary = "LLM 프롬프트 템플릿 수정")
    @PutMapping("/{code}")
    fun updateTemplate(
        authentication: Authentication,
        @PathVariable code: String,
        @Valid @RequestBody request: AdminPromptTemplateUpdateRequest
    ): AdminPromptTemplateResponse {
        authentication.requireAdmin(adminProperties)
        return adminPromptService.updateTemplate(code, request)
    }

    @Operation(summary = "LLM 프롬프트 템플릿 기본값으로 리셋")
    @PostMapping("/{code}/reset")
    fun resetTemplate(
        authentication: Authentication,
        @PathVariable code: String
    ): AdminPromptTemplateResponse {
        authentication.requireAdmin(adminProperties)
        return adminPromptService.resetTemplate(code)
    }

    @Operation(summary = "LLM 프롬프트 템플릿 비활성화")
    @DeleteMapping("/{code}")
    fun disableTemplate(
        authentication: Authentication,
        @PathVariable code: String
    ): AdminPromptTemplateResponse {
        authentication.requireAdmin(adminProperties)
        return adminPromptService.disableTemplate(code)
    }

    @Operation(summary = "상담 요청 기준 최종 LLM 프롬프트 미리보기")
    @PostMapping("/preview")
    fun previewConsultingPrompt(
        authentication: Authentication,
        @Valid @RequestBody request: ConsultRequest
    ): AdminConsultingPromptPreviewResponse {
        authentication.requireAdmin(adminProperties)
        return adminPromptService.previewConsultingPrompt(request)
    }
}
