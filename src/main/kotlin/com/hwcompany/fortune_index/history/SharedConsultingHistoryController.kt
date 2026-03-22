package com.hwcompany.fortune_index.history

import com.hwcompany.fortune_index.auth.requireAuthenticatedUser
import com.hwcompany.fortune_index.auth.requireSameUserId
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.security.core.Authentication
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/history")
@Tag(name = "종합 상담 히스토리 API", description = "상담 이력 상세 및 공유 조회 기능")
class SharedConsultingHistoryController(
    private val consultingHistoryService: ConsultingHistoryService
) {
    @Operation(summary = "종합 상담 히스토리 상세 조회")
    @GetMapping("/{historyId}")
    fun getHistoryDetail(
        authentication: Authentication,
        @PathVariable historyId: Long,
        @RequestParam(required = false) userId: Long?
    ): SharedConsultingHistoryResponse {
        val requestedUserId = userId ?: authentication.requireAuthenticatedUser().userId
        authentication.requireSameUserId(requestedUserId)
        return consultingHistoryService.getHybridHistoryDetail(historyId, requestedUserId)
    }

    @Operation(summary = "공유 종합 상담 히스토리 조회")
    @GetMapping("/share/{shareKey}")
    fun getSharedHistory(@PathVariable shareKey: String): SharedConsultingHistoryResponse =
        consultingHistoryService.getSharedHistory(shareKey)
}
