package com.hwcompany.fortune_index.zodiac

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/zodiac-fortune")
@Tag(name = "별자리 운세 API", description = "서울 기준 오늘의 공통 별자리 운세를 반환한다.")
class TodayZodiacFortuneController(
    private val todayZodiacFortuneService: TodayZodiacFortuneService
) {
    @Operation(summary = "오늘의 별자리 운세 조회")
    @GetMapping("/today")
    fun getTodayFortune(): TodayZodiacFortuneResponse =
        todayZodiacFortuneService.getTodayFortune()
}
