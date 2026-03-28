package com.hwcompany.fortune_index.auth.email

import java.time.LocalDateTime
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import org.mockito.Mockito.mock
import org.mockito.Mockito.never
import org.mockito.Mockito.times
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import org.springframework.http.HttpStatus
import org.springframework.web.server.ResponseStatusException

class EmailVerificationServiceTest {
    private val repository = mock(EmailVerificationRepository::class.java)
    private val mailSender = RecordingMailSender()
    private val tokenGenerator = EmailVerificationTokenGenerator { "generated-token" }
    private val properties = EmailVerificationProperties(
        baseUrl = "https://your-domain.com",
        verifyPath = "/email/verify",
        expirationMinutes = 15
    )

    private val service = EmailVerificationService(
        emailVerificationRepository = repository,
        emailVerificationMailSender = mailSender,
        emailVerificationTokenGenerator = tokenGenerator,
        properties = properties
    )

    @Test
    fun `이미 인증된 이메일은 재요청해도 메일을 보내지 않는다`() {
        `when`(repository.findTopByEmailOrderByRequestedAtDescIdDesc("user@example.com")).thenReturn(
            EmailVerification(
                id = 1L,
                email = "user@example.com",
                token = "verified-token",
                status = EmailVerificationStatus.VERIFIED,
                requestedAt = LocalDateTime.now(),
                expiresAt = LocalDateTime.now().plusMinutes(15),
                verifiedAt = LocalDateTime.now()
            )
        )

        service.requestVerification(" user@example.com ")

        verify(repository, never()).save(org.mockito.ArgumentMatchers.any(EmailVerification::class.java))
        assertEquals(0, mailSender.sentCount)
    }

    @Test
    fun `인증 요청은 새 토큰을 저장하고 메일을 보낸다`() {
        `when`(repository.findTopByEmailOrderByRequestedAtDescIdDesc("user@example.com")).thenReturn(null)
        `when`(repository.save(org.mockito.ArgumentMatchers.any(EmailVerification::class.java))).thenAnswer { invocation ->
            invocation.getArgument<EmailVerification>(0).copy(id = 10L)
        }

        service.requestVerification("user@example.com")

        verify(repository, times(1)).save(org.mockito.ArgumentMatchers.any(EmailVerification::class.java))
        assertEquals(1, mailSender.sentCount)
        assertEquals("user@example.com", mailSender.lastEmail)
        assertEquals("https://your-domain.com/email/verify?token=generated-token", mailSender.lastUrl)
    }

    @Test
    fun `최신 토큰이 아니면 만료 페이지를 반환한다`() {
        val oldVerification = EmailVerification(
            id = 1L,
            email = "user@example.com",
            token = "old-token",
            status = EmailVerificationStatus.PENDING,
            requestedAt = LocalDateTime.now().minusMinutes(2),
            expiresAt = LocalDateTime.now().plusMinutes(13)
        )
        val latestVerification = EmailVerification(
            id = 2L,
            email = "user@example.com",
            token = "latest-token",
            status = EmailVerificationStatus.PENDING,
            requestedAt = LocalDateTime.now().minusMinutes(1),
            expiresAt = LocalDateTime.now().plusMinutes(14)
        )

        `when`(repository.findByToken("old-token")).thenReturn(oldVerification)
        `when`(repository.findTopByEmailOrderByRequestedAtDescIdDesc("user@example.com")).thenReturn(latestVerification)

        val result = service.verifyToken("old-token")

        assertEquals(EmailVerificationResult.EXPIRED, result)
    }

    @Test
    fun `유효한 최신 토큰은 VERIFIED 로 전환한다`() {
        val verification = EmailVerification(
            id = 1L,
            email = "user@example.com",
            token = "valid-token",
            status = EmailVerificationStatus.PENDING,
            requestedAt = LocalDateTime.now().minusMinutes(1),
            expiresAt = LocalDateTime.now().plusMinutes(14)
        )

        `when`(repository.findByToken("valid-token")).thenReturn(verification)
        `when`(repository.findTopByEmailOrderByRequestedAtDescIdDesc("user@example.com")).thenReturn(verification)

        val result = service.verifyToken("valid-token")

        assertEquals(EmailVerificationResult.SUCCESS, result)
        assertEquals(EmailVerificationStatus.VERIFIED, verification.status)
    }

    @Test
    fun `만료된 토큰은 EXPIRED 를 반환한다`() {
        val verification = EmailVerification(
            id = 1L,
            email = "user@example.com",
            token = "expired-token",
            status = EmailVerificationStatus.PENDING,
            requestedAt = LocalDateTime.now().minusMinutes(30),
            expiresAt = LocalDateTime.now().minusMinutes(15)
        )

        `when`(repository.findByToken("expired-token")).thenReturn(verification)
        `when`(repository.findTopByEmailOrderByRequestedAtDescIdDesc("user@example.com")).thenReturn(verification)

        val result = service.verifyToken("expired-token")

        assertEquals(EmailVerificationResult.EXPIRED, result)
    }

    @Test
    fun `상태 조회는 최신 상태를 반환한다`() {
        `when`(repository.findTopByEmailOrderByRequestedAtDescIdDesc("user@example.com")).thenReturn(
            EmailVerification(
                id = 1L,
                email = "user@example.com",
                token = "verified-token",
                status = EmailVerificationStatus.VERIFIED,
                requestedAt = LocalDateTime.now(),
                expiresAt = LocalDateTime.now().plusMinutes(15),
                verifiedAt = LocalDateTime.now()
            )
        )

        val status = service.getLatestStatus("user@example.com")

        assertEquals(EmailVerificationStatus.VERIFIED, status)
    }

    @Test
    fun `상태 조회 대상이 없으면 404 를 반환한다`() {
        `when`(repository.findTopByEmailOrderByRequestedAtDescIdDesc("missing@example.com")).thenReturn(null)

        val exception = assertFailsWith<ResponseStatusException> {
            service.getLatestStatus("missing@example.com")
        }

        assertEquals(HttpStatus.NOT_FOUND, exception.statusCode)
    }
}

private class RecordingMailSender : EmailVerificationMailSender {
    var sentCount: Int = 0
    var lastEmail: String? = null
    var lastUrl: String? = null

    override fun send(email: String, verificationUrl: String) {
        sentCount += 1
        lastEmail = email
        lastUrl = verificationUrl
    }
}
