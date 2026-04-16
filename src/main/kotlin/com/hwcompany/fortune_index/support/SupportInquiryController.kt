package com.hwcompany.fortune_index.support

import com.hwcompany.fortune_index.auth.AuthenticatedUser
import com.hwcompany.fortune_index.auth.MessageResponse
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.security.core.Authentication
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/support/inquiries")
@Tag(name = "문의하기 API", description = "로그인 사용자의 문의 등록")
class SupportInquiryController(
    private val supportInquiryService: SupportInquiryService
) {
    @Operation(summary = "문의 등록")
    @PostMapping
    fun createInquiry(
        authentication: Authentication,
        @RequestBody request: CreateSupportInquiryRequest
    ): MessageResponse =
        supportInquiryService.createInquiry(authentication.requireAuthenticatedUser(), request)
}

private fun Authentication.requireAuthenticatedUser(): AuthenticatedUser =
    principal as? AuthenticatedUser ?: error("AuthenticatedUser principal is missing")
