package com.hwcompany.fortune_index.auth

import com.hwcompany.fortune_index.domain.model.SajuResult
import org.springframework.data.jpa.repository.JpaRepository

interface AuthTestSajuRepository : JpaRepository<SajuResult, Long> {
    fun findTopByUserEmailOrderByAnalyzedAtDesc(email: String): SajuResult?
}
