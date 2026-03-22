package com.hwcompany.fortune_index.profile

import com.hwcompany.fortune_index.auth.requireAuthenticatedUser
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.security.core.Authentication
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/users/me")
@Tag(name = "마이페이지 프로필 API", description = "마이페이지 프로필 상세 조회")
class ProfileDetailsController(
    private val profileDetailsService: ProfileDetailsService
) {
    @Operation(summary = "내 프로필 상세 조회", description = "생일 타로 카드와 사주 상세 정보를 조회한다.")
    @GetMapping("/profile-details")
    fun getProfileDetails(authentication: Authentication): MyProfileDetailsResponse =
        profileDetailsService.getProfileDetails(authentication.requireAuthenticatedUser().userId)
}
