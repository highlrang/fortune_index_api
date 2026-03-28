package com.hwcompany.fortune_index.investment

import com.hwcompany.fortune_index.auth.requireSameUserId
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotBlank
import java.time.LocalDateTime
import org.springframework.security.core.Authentication
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/users/{userId}/virtual-investments")
@Tag(name = "가상투자 API", description = "버튼 클릭 시점의 실제 시장가를 기록하고 이후 수익률을 추적하는 기능")
class VirtualInvestmentController(
    private val virtualInvestmentService: VirtualInvestmentService
) {
    @Operation(summary = "가상투자 기록")
    @PostMapping
    fun createVirtualInvestment(
        authentication: Authentication,
        @PathVariable userId: Long,
        @Valid @RequestBody request: CreateVirtualInvestmentRequest
    ): VirtualInvestmentPositionResponse {
        authentication.requireSameUserId(userId)
        return virtualInvestmentService.buy(
            BuyVirtualInvestmentRequest(
                userId = userId,
                stockCode = request.stockCode,
                buyQuantity = request.buyQuantity,
                boughtAt = request.boughtAt ?: LocalDateTime.now(),
                isHolding = true
            )
        )
    }

    @Operation(summary = "가상투자 목록 조회")
    @GetMapping
    fun getVirtualInvestments(
        authentication: Authentication,
        @PathVariable userId: Long,
        @RequestParam(defaultValue = "false") holdingOnly: Boolean
    ): List<VirtualInvestmentPositionResponse> {
        authentication.requireSameUserId(userId)
        return virtualInvestmentService.getUserVirtualInvestments(userId, holdingOnly = holdingOnly)
    }
}

data class CreateVirtualInvestmentRequest(
    @field:NotBlank
    val stockCode: String,
    @field:Min(1)
    val buyQuantity: Long,
    val boughtAt: LocalDateTime? = null
)
