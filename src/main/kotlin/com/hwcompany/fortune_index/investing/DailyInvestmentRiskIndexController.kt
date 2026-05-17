package com.hwcompany.fortune_index.investing

import com.hwcompany.fortune_index.auth.AuthenticatedUser
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.HttpStatus
import org.springframework.security.core.Authentication
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.server.ResponseStatusException

@RestController
@RequestMapping("/api")
@Tag(name = "투자 리스크 지수 API", description = "사용자별 당일 투자 에너지 긴장도 지수를 개별 조회하는 기능")
class DailyInvestmentRiskIndexController(
    private val dailyInvestmentRiskIndexService: DailyInvestmentRiskIndexService
) {
    @Operation(summary = "당일 투자 리스크 지수 조회")
    @GetMapping("/daily-risk-index")
    fun getDailyRiskIndex(
        authentication: Authentication,
        @RequestParam userId: Long
    ): DailyInvestmentRiskIndexResponse {
        val authenticatedUser = authentication.principal as? AuthenticatedUser
            ?: error("AuthenticatedUser principal is missing")
        if (authenticatedUser.userId != userId) {
            throw ResponseStatusException(HttpStatus.FORBIDDEN, "cannot access another user's resource")
        }
        return dailyInvestmentRiskIndexService.computeForUser(userId)
    }
}
