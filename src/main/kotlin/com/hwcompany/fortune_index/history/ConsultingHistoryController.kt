package com.hwcompany.fortune_index.history

import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.web.PageableDefault
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/users/{userId}/consulting-histories")
class ConsultingHistoryController(
    private val consultingHistoryService: ConsultingHistoryService
) {
    @GetMapping
    fun getHistories(
        @PathVariable userId: Long,
        @PageableDefault(size = 20) pageable: Pageable
    ): Page<ConsultingHistorySummaryResponse> =
        consultingHistoryService.getHistories(userId, pageable)

    @GetMapping("/{historyId}")
    fun getHistoryDetail(
        @PathVariable userId: Long,
        @PathVariable historyId: Long
    ): ConsultingHistoryDetailResponse =
        consultingHistoryService.getHistoryDetail(userId, historyId)

    @PatchMapping("/{historyId}/retro")
    fun updateRetro(
        @PathVariable userId: Long,
        @PathVariable historyId: Long,
        @RequestBody request: UpdateConsultingRetroRequest
    ): ConsultingHistoryDetailResponse =
        consultingHistoryService.updateRetro(userId, historyId, request)

    @GetMapping("/retro/stats")
    fun getRetroStats(
        @PathVariable userId: Long
    ): ConsultingRetroStatsResponse =
        consultingHistoryService.getRetroStats(userId)
}
