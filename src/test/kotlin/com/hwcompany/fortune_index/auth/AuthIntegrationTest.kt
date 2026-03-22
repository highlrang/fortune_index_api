package com.hwcompany.fortune_index.auth

import com.hwcompany.fortune_index.domain.model.EmailVerificationPurpose
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.hamcrest.Matchers.not
import org.hamcrest.Matchers.blankOrNullString
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.MediaType
import org.springframework.transaction.annotation.Transactional
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.get
import org.springframework.test.web.servlet.post
import org.springframework.test.web.servlet.delete

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("h2")
@Transactional
class AuthIntegrationTest(
    @Autowired private val mockMvc: MockMvc,
    @Autowired private val emailVerificationTokenRepository: EmailVerificationTokenRepository,
    @Autowired private val sajuResultRepository: AuthTestSajuRepository
) {
    @Test
    fun `signup login token auth and password reset flow works`() {
        val email = "tester@example.com"

        mockMvc.get("/api/auth/me").andExpect {
            status { isUnauthorized() }
        }

        mockMvc.delete("/api/auth/me") {
            contentType = MediaType.APPLICATION_JSON
            content = """{"password":"Password123!"}"""
        }.andExpect {
            status { isUnauthorized() }
        }

        mockMvc.post("/api/auth/signup/email/request") {
            contentType = MediaType.APPLICATION_JSON
            content = """{"email":"$email"}"""
        }.andExpect {
            status { isOk() }
        }

        val signupCode = emailVerificationTokenRepository
            .findFirstByEmailAndPurposeOrderByCreatedAtDesc(email, EmailVerificationPurpose.SIGNUP)
            ?.verificationCode
        assertThat(signupCode).isNotBlank()

        mockMvc.post("/api/auth/signup") {
            contentType = MediaType.APPLICATION_JSON
            content = """
                {
                  "name":"Tester",
                  "email":"$email",
                  "password":"Password123!",
                  "verificationCode":"$signupCode",
                  "birthDate":"1990-01-01",
                  "investmentRiskProfile":"STABLE",
                  "preferredSectors":["TECHNOLOGY","ETF"]
                }
            """.trimIndent()
        }.andExpect {
            status { isOk() }
            jsonPath("$.user.email") { value(email) }
            jsonPath("$.tokens.accessToken", not(blankOrNullString()))
            jsonPath("$.tokens.refreshToken", not(blankOrNullString()))
        }

        val sajuResult = sajuResultRepository.findTopByUserEmailOrderByAnalyzedAtDesc(email)
        assertThat(sajuResult).isNotNull
        assertThat(sajuResult?.heavenlyStems).hasSize(4)
        assertThat(sajuResult?.earthlyBranches).hasSize(4)
        assertThat(sajuResult?.heavenlyStems?.all { it.code.isNotBlank() && it.labelKo.isNotBlank() && it.sortOrder > 0 }).isTrue()
        assertThat(sajuResult?.heavenlyStems?.map { it.pillarOrder }).containsExactly(1, 2, 3, 4)
        assertThat(sajuResult?.earthlyBranches?.all { it.code.isNotBlank() && it.labelKo.isNotBlank() && it.sortOrder > 0 }).isTrue()
        assertThat(sajuResult?.earthlyBranches?.map { it.pillarOrder }).containsExactly(1, 2, 3, 4)
        assertThat(sajuResult?.fiveElements?.wood).isNotNull()

        val loginResponse = mockMvc.post("/api/auth/login") {
            contentType = MediaType.APPLICATION_JSON
            content = """{"email":"$email","password":"Password123!"}"""
        }.andExpect {
            status { isOk() }
        }.andReturn()

        val accessToken = JsonTestUtils.readJson(loginResponse.response.contentAsString, "$.tokens.accessToken")

        mockMvc.get("/api/auth/me") {
            header("Authorization", "Bearer $accessToken")
        }.andExpect {
            status { isOk() }
            jsonPath("$.id") { exists() }
            jsonPath("$.name") { value("Tester") }
            jsonPath("$.email") { value(email) }
            jsonPath("$.emailVerified") { value(true) }
            jsonPath("$.investmentRiskProfile") { value("STABLE") }
            jsonPath("$.preferredSectors[0]") { value("ETF") }
            jsonPath("$.preferredSectors[1]") { value("TECHNOLOGY") }
        }

        mockMvc.delete("/api/auth/me") {
            header("Authorization", "Bearer $accessToken")
            contentType = MediaType.APPLICATION_JSON
            content = """{"password":"Password123!"}"""
        }.andExpect {
            status { isOk() }
            jsonPath("$.message") { value("account withdrawn") }
        }

        mockMvc.get("/api/auth/me") {
            header("Authorization", "Bearer $accessToken")
        }.andExpect {
            status { isForbidden() }
        }

        mockMvc.post("/api/auth/login") {
            contentType = MediaType.APPLICATION_JSON
            content = """{"email":"$email","password":"Password123!"}"""
        }.andExpect {
            status { isUnauthorized() }
        }

        mockMvc.post("/api/auth/password-reset/request") {
            contentType = MediaType.APPLICATION_JSON
            content = """{"email":"$email"}"""
        }.andExpect {
            status { isNotFound() }
        }
    }
}
