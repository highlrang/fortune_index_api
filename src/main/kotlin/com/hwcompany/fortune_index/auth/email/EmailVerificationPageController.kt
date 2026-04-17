package com.hwcompany.fortune_index.auth.email

import com.hwcompany.fortune_index.auth.AuthService
import jakarta.validation.constraints.NotBlank
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@Validated
@RestController
@RequestMapping("/email")
class EmailVerificationPageController(
    private val authService: AuthService,
    private val emailVerificationPageRenderer: EmailVerificationPageRenderer
) {
    @GetMapping(
        "/verify",
        produces = [MediaType.TEXT_HTML_VALUE]
    )
    fun verifyEmail(
        @RequestParam @NotBlank token: String
    ): ResponseEntity<String> {
        val result = authService.verifyEmailToken(token)
            ?: EmailVerificationOutcome(EmailVerificationResult.FAILURE)
        return ResponseEntity.ok()
            .contentType(MediaType.TEXT_HTML)
            .body(emailVerificationPageRenderer.render(result))
    }
}
