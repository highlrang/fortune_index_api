package com.hwcompany.fortune_index.condition

import com.hwcompany.fortune_index.auth.requireAuthenticatedUser
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.security.core.Authentication
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/investment-condition")
@Tag(name = "투자 컨디션 API", description = "일별 투자 컨디션 조회 및 사용자 변경 저장")
class InvestmentConditionController(
    private val investmentConditionService: InvestmentConditionService
) {
    @Operation(summary = "오늘의 투자 컨디션 조회")
    @GetMapping("/today")
    fun getTodayCondition(authentication: Authentication): InvestmentConditionResponse =
        investmentConditionService.getTodayCondition(authentication.requireAuthenticatedUser())

    @Operation(summary = "오늘의 투자 컨디션 변경")
    @PatchMapping("/today")
    fun updateTodayCondition(
        authentication: Authentication,
        @Valid @RequestBody request: UpdateInvestmentConditionRequest
    ): InvestmentConditionResponse =
        investmentConditionService.updateTodayCondition(authentication.requireAuthenticatedUser(), request)
}
