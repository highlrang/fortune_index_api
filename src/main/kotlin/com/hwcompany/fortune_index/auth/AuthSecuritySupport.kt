package com.hwcompany.fortune_index.auth

import com.hwcompany.fortune_index.admin.AdminProperties
import org.springframework.http.HttpStatus
import org.springframework.security.core.Authentication
import org.springframework.web.server.ResponseStatusException

fun Authentication.requireAuthenticatedUser(): AuthenticatedUser =
    principal as? AuthenticatedUser ?: error("인증 사용자 정보를 찾을 수 없습니다.")

fun Authentication.requireSameUserId(targetUserId: Long): AuthenticatedUser {
    val authenticatedUser = requireAuthenticatedUser()
    if (authenticatedUser.userId != targetUserId) {
        throw ResponseStatusException(HttpStatus.FORBIDDEN, "다른 사용자의 리소스에 접근할 수 없습니다.")
    }
    return authenticatedUser
}

fun Authentication.requireAdmin(adminProperties: AdminProperties): AuthenticatedUser {
    val authenticatedUser = requireAuthenticatedUser()
    if (!adminProperties.isAdmin(authenticatedUser.email)) {
        throw ResponseStatusException(HttpStatus.FORBIDDEN, "관리자 권한이 필요합니다.")
    }
    return authenticatedUser
}
