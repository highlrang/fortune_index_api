package com.hwcompany.fortune_index.auth.email

import jakarta.validation.Valid
import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotBlank
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@Validated
@RestController
@RequestMapping("/auth/email")
class EmailVerificationController(
    private val emailVerificationService: EmailVerificationService
) {
    @PostMapping(
        "/request",
        consumes = [MediaType.APPLICATION_JSON_VALUE]
    )
    fun requestVerification(
        @Valid @RequestBody request: EmailVerificationRequest
    ): ResponseEntity<Void> {
        emailVerificationService.requestVerification(request.email)
        return ResponseEntity.ok().build()
    }

    @GetMapping("/status")
    fun getStatus(
        @RequestParam @NotBlank @Email email: String
    ): EmailVerificationStatusResponse =
        EmailVerificationStatusResponse(
            status = emailVerificationService.getLatestStatus(email).name
        )
}

data class EmailVerificationRequest(
    @field:NotBlank
    @field:Email
    val email: String
)

data class EmailVerificationStatusResponse(
    val status: String
)
