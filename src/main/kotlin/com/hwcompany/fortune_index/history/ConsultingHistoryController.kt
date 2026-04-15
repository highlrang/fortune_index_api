package com.hwcompany.fortune_index.history

import com.hwcompany.fortune_index.auth.requireSameUserId
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import java.time.LocalDate
import org.springframework.format.annotation.DateTimeFormat
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.web.PageableDefault
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.security.core.Authentication

@RestController
@RequestMapping("/api/users/{userId}/consulting-histories")
@Tag(name = "운세 이력 API", description = "사용자 재물 운세 상담 이력 조회 및 회고 관리 기능")
class ConsultingHistoryController(
    private val consultingHistoryService: ConsultingHistoryService
) {
    @Operation(summary = "좋아요한 운세 상담 이력 목록 조회")
    @GetMapping("/liked")
    fun getLikedHistories(
        authentication: Authentication,
        @PathVariable userId: Long,
        @PageableDefault(size = 20) pageable: Pageable
    ): Page<ConsultingHistorySummaryResponse> {
        authentication.requireSameUserId(userId)
        return consultingHistoryService.getHelpfulHistories(userId, pageable)
    }

    @Operation(summary = "운세 상담 좋아요 추가")
    @PostMapping("/{historyId}/liked")
    fun likeHistory(
        authentication: Authentication,
        @PathVariable userId: Long,
        @PathVariable historyId: Long
    ): ConsultingHistoryLikeResponse {
        authentication.requireSameUserId(userId)
        return consultingHistoryService.likeHistory(userId, historyId)
    }

    @Operation(summary = "운세 상담 좋아요 취소")
    @DeleteMapping("/{historyId}/liked")
    fun unlikeHistory(
        authentication: Authentication,
        @PathVariable userId: Long,
        @PathVariable historyId: Long
    ): ConsultingHistoryLikeResponse {
        authentication.requireSameUserId(userId)
        return consultingHistoryService.unlikeHistory(userId, historyId)
    }

    @Operation(summary = "날짜별 운세 상담 이력 목록 조회")
    @GetMapping
    fun getHistories(
        authentication: Authentication,
        @PathVariable userId: Long,
        @PageableDefault(size = 20) pageable: Pageable
    ): Page<ConsultingHistoryDateSummaryResponse> {
        authentication.requireSameUserId(userId)
        return consultingHistoryService.getHistoryDates(userId, pageable)
    }

    @Operation(summary = "특정 날짜 운세 상담 이력 상세 조회")
    @GetMapping("/by-date")
    fun getHistoryDetailsByDate(
        authentication: Authentication,
        @PathVariable userId: Long,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) date: LocalDate
    ): ConsultingHistoryDateDetailResponse {
        authentication.requireSameUserId(userId)
        return consultingHistoryService.getHistoryDetailsByDate(userId, date)
    }

    @Operation(summary = "운세 상담 리뷰 작성 및 수정")
    @PatchMapping("/{historyId}/review")
    fun updateReview(
        authentication: Authentication,
        @PathVariable userId: Long,
        @PathVariable historyId: Long,
        @RequestBody request: UpdateConsultingReviewRequest
    ): ConsultingHistoryReviewResponse {
        authentication.requireSameUserId(userId)
        return consultingHistoryService.updateReview(userId, historyId, request)
    }
}
