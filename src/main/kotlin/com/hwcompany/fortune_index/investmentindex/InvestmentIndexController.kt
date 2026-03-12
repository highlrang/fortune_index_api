package com.hwcompany.fortune_index.investmentindex

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/investment-index")
@Tag(name = "투자 지수 API", description = "실시간 시장과 운세 기반의 하이브리드 투자 지수")
class InvestmentIndexController(
    private val investmentIndexService: InvestmentIndexService
) {
    @Operation(summary = "오늘의 투자 지수 조회", description = "한국 시간 기준으로 현재 시장과 운세 점수를 합산한 지수를 반환한다.")
    @GetMapping
    fun getInvestmentIndex(): TotalIndexResponse =
        investmentIndexService.getInvestmentIndex()
}

