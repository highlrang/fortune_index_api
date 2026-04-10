package com.hwcompany.fortune_index.home

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/home")
@Tag(name = "홈 API", description = "홈 화면 전용 정규화 응답")
class HomeController(
    private val homeService: HomeService
) {
    @Operation(summary = "홈 요약 조회", description = "오늘의 흐름 점수와 홈 화면 요약 정보를 반환한다.")
    @GetMapping("/summary")
    fun getSummary(): HomeSummaryResponse =
        homeService.getSummary()

    @Operation(summary = "홈 지수 차트 조회", description = "홈 화면 차트용 지수 시계열을 정규화해서 반환한다.")
    @GetMapping("/index-chart")
    fun getIndexChart(
        @RequestParam indexCode: String,
        @RequestParam(defaultValue = "1D") period: String
    ): HomeIndexChartResponse =
        homeService.getIndexChart(indexCode = indexCode, period = period)
}
