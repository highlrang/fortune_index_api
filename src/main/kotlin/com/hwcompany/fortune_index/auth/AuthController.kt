package com.hwcompany.fortune_index.auth

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.security.core.Authentication
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/auth")
@Tag(name = "인증 API", description = "회원가입, 로그인, 로그아웃, 토큰 재발급, 비밀번호 재설정")
class AuthController(
    private val authService: AuthService
) {
    @Operation(summary = "회원가입 이메일 인증 링크 요청")
    @PostMapping("/signup/email/request")
    fun requestSignupEmailVerification(@Valid @RequestBody request: EmailVerificationLinkRequest): EmailVerificationLinkResponse =
        authService.requestSignupEmailVerification(request)

    @Operation(summary = "회원가입")
    @PostMapping("/signup")
    fun signUp(@Valid @RequestBody request: SignUpRequest): AuthResponse =
        authService.signUp(request)

    @Operation(summary = "로그인")
    @PostMapping("/login")
    fun login(@Valid @RequestBody request: LoginRequest): AuthResponse =
        authService.login(request)

    @Operation(summary = "Access/Refresh 토큰 재발급")
    @PostMapping("/refresh")
    fun refresh(@Valid @RequestBody request: TokenRefreshRequest): AuthResponse =
        authService.refresh(request)

    @Operation(summary = "로그아웃")
    @PostMapping("/logout")
    fun logout(@Valid @RequestBody request: LogoutRequest): MessageResponse {
        authService.logout(request)
        return MessageResponse("logged out")
    }

    @Operation(summary = "현재 로그인 사용자 조회")
    @GetMapping("/me")
    fun me(authentication: Authentication): CurrentUserResponse =
        authService.getCurrentUser(authentication.requireAuthenticatedUser())

    @Operation(summary = "비밀번호 재설정 이메일 인증 링크 요청")
    @PostMapping("/password-reset/request")
    fun requestPasswordResetEmailVerification(@Valid @RequestBody request: EmailVerificationLinkRequest): EmailVerificationLinkResponse =
        authService.requestPasswordResetEmailVerification(request)

    @Operation(summary = "비밀번호 재설정 완료")
    @PostMapping("/password-reset/confirm")
    fun confirmPasswordReset(@Valid @RequestBody request: PasswordResetConfirmRequest): MessageResponse {
        authService.confirmPasswordReset(request)
        return MessageResponse("password reset completed")
    }

    @Operation(summary = "회원 탈퇴")
    @DeleteMapping("/me")
    fun withdraw(
        authentication: Authentication,
        @Valid @RequestBody request: WithdrawRequest
    ): MessageResponse {
        authService.withdraw(authentication.requireAuthenticatedUser(), request)
        return MessageResponse("account withdrawn")
    }
}
