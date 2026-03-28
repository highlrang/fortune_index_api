package com.hwcompany.fortune_index.auth.email

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
    private val emailVerificationService: EmailVerificationService,
    private val emailVerificationPageRenderer: EmailVerificationPageRenderer
) {
    @GetMapping(
        "/verify",
        produces = [MediaType.TEXT_HTML_VALUE]
    )
    fun verifyEmail(
        @RequestParam @NotBlank token: String
    ): ResponseEntity<String> {
        val result = emailVerificationService.verifyToken(token)
        return ResponseEntity.ok()
            .contentType(MediaType.TEXT_HTML)
            .body(emailVerificationPageRenderer.render(result))
    }
}
