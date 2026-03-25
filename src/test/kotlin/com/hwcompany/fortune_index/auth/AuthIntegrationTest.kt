package com.hwcompany.fortune_index.auth

import com.hwcompany.fortune_index.domain.model.EmailVerificationPurpose
import org.assertj.core.api.Assertions.assertThat
import org.hamcrest.Matchers.endsWith
import org.junit.jupiter.api.Test
import org.hamcrest.Matchers.not
import org.hamcrest.Matchers.blankOrNullString
import org.hamcrest.Matchers.nullValue
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
                  "birthTime":"08:30:00",
                  "gender":"F",
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
            jsonPath("$.birthDate") { value("1990-01-01") }
            jsonPath("$.birthTime") { value("08:30:00") }
            jsonPath("$.gender") { value("F") }
            jsonPath("$.profileImageUrl") { value(nullValue()) }
            jsonPath("$.notificationEnabled") { value(true) }
            jsonPath("$.virtualInvestmentEnabled") { value(false) }
            jsonPath("$.darkModeEnabled") { value(true) }
            jsonPath("$.createdAt") { value(endsWith("Z")) }
            jsonPath("$.lastLoginAt") { value(endsWith("Z")) }
        }

        mockMvc.get("/api/users/me/profile-details") {
            header("Authorization", "Bearer $accessToken")
        }.andExpect {
            status { isOk() }
            jsonPath("$.birthTarot.name") { value("The World") }
            jsonPath("$.birthTarot.koreanName") { value("세계") }
            jsonPath("$.birthTarot.number") { value(21) }
            jsonPath("$.birthTarot.meaning") { isNotEmpty() }
            jsonPath("$.birthTarot.imageUrl") { isNotEmpty() }
            jsonPath("$.saju.palza.length()") { value(4) }
            jsonPath("$.saju.ohang.wood") { exists() }
            jsonPath("$.saju.ohang.fire") { exists() }
            jsonPath("$.saju.ohang.earth") { exists() }
            jsonPath("$.saju.ohang.metal") { exists() }
            jsonPath("$.saju.ohang.water") { exists() }
            jsonPath("$.saju.ilju.name") { isNotEmpty() }
            jsonPath("$.saju.ilju.summary") { isNotEmpty() }
            jsonPath("$.saju.wolji.name") { isNotEmpty() }
            jsonPath("$.saju.wolji.summary") { isNotEmpty() }
            jsonPath("$.saju.daeun.name") { isNotEmpty() }
            jsonPath("$.saju.daeun.summary") { isNotEmpty() }
            jsonPath("$.saju.sewun.name") { isNotEmpty() }
            jsonPath("$.saju.sewun.summary") { isNotEmpty() }
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
