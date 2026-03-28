package com.hwcompany.fortune_index.profile

import com.hwcompany.fortune_index.auth.AuthService
import com.hwcompany.fortune_index.auth.CurrentUserResponse
import com.hwcompany.fortune_index.auth.UpdateCurrentUserRequest
import com.hwcompany.fortune_index.auth.requireAuthenticatedUser
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.security.core.Authentication
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/users/me")
@Tag(name = "내 정보 API", description = "현재 로그인 사용자 정보 조회 및 수정")
class MyProfileController(
    private val authService: AuthService
) {
    @Operation(summary = "내 정보 수정")
    @PatchMapping
    fun updateCurrentUser(
        authentication: Authentication,
        @Valid @RequestBody request: UpdateCurrentUserRequest
    ): CurrentUserResponse =
        authService.updateCurrentUser(authentication.requireAuthenticatedUser(), request)
}
