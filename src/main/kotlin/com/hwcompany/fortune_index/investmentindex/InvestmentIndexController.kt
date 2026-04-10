package com.hwcompany.fortune_index.investmentindex

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/investment-index")
@Tag(name = "오늘의 흐름 점수 API", description = "사주와 타로를 바탕으로 오늘의 재물 흐름 점수를 보여준다.")
class InvestmentIndexController(
    private val investmentIndexService: InvestmentIndexService
) {
    @Operation(summary = "오늘의 흐름 점수 조회", description = "한국 시간 기준으로 오늘의 재물 흐름 점수를 반환한다.")
    @GetMapping
    fun getInvestmentIndex(): TotalIndexResponse =
        investmentIndexService.getInvestmentIndex()
}
