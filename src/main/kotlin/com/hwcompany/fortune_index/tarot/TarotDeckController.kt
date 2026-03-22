package com.hwcompany.fortune_index.tarot

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/tarot")
@Tag(name = "타로 덱 API", description = "타로 덱 버전 및 카드 메타데이터 조회")
class TarotDeckController(
    private val tarotDeckService: TarotDeckService
) {
    @Operation(summary = "타로 덱 버전 목록 조회", description = "활성화된 타로 덱 버전만 반환한다.")
    @GetMapping("/deck-versions")
    fun getDeckVersions(): List<TarotDeckVersionSummary> =
        tarotDeckService.getDeckVersions()

    @Operation(
        summary = "덱 버전별 카드 메타데이터 조회",
        description = "selectedIndices가 있으면 요청한 순서대로 반환한다. selectedIndex는 덱 내 고정 카드 인덱스다."
    )
    @GetMapping("/deck-versions/{deckVersionId}/cards")
    fun getDeckCards(
        @PathVariable deckVersionId: String,
        @RequestParam(required = false) selectedIndices: List<Int>?
    ): List<TarotCardMetadata> =
        tarotDeckService.getDeckCards(deckVersionId, selectedIndices)
}
