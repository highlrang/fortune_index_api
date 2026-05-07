package com.hwcompany.fortune_index.home

import com.hwcompany.fortune_index.auth.requireAuthenticatedUser
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.security.core.Authentication
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/home")
@Tag(name = "홈 API", description = "홈 화면 전용 정규화 응답")
class HomeController(
    private val homeService: HomeService
) {
    @Operation(summary = "홈 요약 조회", description = "오늘의 사주, 타로, 별자리 요약 정보를 반환한다.")
    @GetMapping("/summary")
    fun getSummary(): HomeSummaryResponse =
        homeService.getSummary()

    @Operation(
        summary = "홈 오늘의 타로 3장 조회",
        description = "오늘 이미 뽑은 홈 타로 카드 3장을 반환한다. 아직 뽑지 않았다면 canDraw=true와 빈 cards를 반환한다."
    )
    @GetMapping("/tarot/daily-draw")
    fun getDailyTarotDraw(authentication: Authentication): HomeDailyTarotDrawResponse =
        homeService.getDailyTarotDraw(authentication.requireAuthenticatedUser())

    @Operation(
        summary = "홈 오늘의 타로 3장 저장",
        description = "사용자가 직접 선택한 홈 타로 카드 3장을 서울 날짜 기준 하루에 한 번만 저장한다."
    )
    @PostMapping("/tarot/daily-draw")
    fun saveDailyTarotCards(
        authentication: Authentication,
        @Valid @RequestBody request: SaveHomeDailyTarotDrawRequest
    ): HomeDailyTarotDrawResponse =
        homeService.saveDailyTarotCards(authentication.requireAuthenticatedUser(), request)
}
