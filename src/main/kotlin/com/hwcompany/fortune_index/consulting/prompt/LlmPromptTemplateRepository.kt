package com.hwcompany.fortune_index.consulting.prompt

import com.hwcompany.fortune_index.domain.model.LlmPromptTemplate
import org.springframework.data.jpa.repository.JpaRepository

interface LlmPromptTemplateRepository : JpaRepository<LlmPromptTemplate, Long> {
    fun findByCode(code: String): LlmPromptTemplate?
    fun findByCodeAndEnabledTrue(code: String): LlmPromptTemplate?
    fun existsByCode(code: String): Boolean
}
