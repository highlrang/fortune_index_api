package com.hwcompany.fortune_index.auth

import com.hwcompany.fortune_index.admin.AdminProperties
import org.springframework.http.HttpStatus
import org.springframework.security.core.Authentication
import org.springframework.web.server.ResponseStatusException

fun Authentication.requireAuthenticatedUser(): AuthenticatedUser =
    principal as? AuthenticatedUser ?: error("AuthenticatedUser principal is missing")

fun Authentication.requireSameUserId(targetUserId: Long): AuthenticatedUser {
    val authenticatedUser = requireAuthenticatedUser()
    if (authenticatedUser.userId != targetUserId) {
        throw ResponseStatusException(HttpStatus.FORBIDDEN, "cannot access another user's resource")
    }
    return authenticatedUser
}

fun Authentication.requireAdmin(adminProperties: AdminProperties): AuthenticatedUser {
    val authenticatedUser = requireAuthenticatedUser()
    if (!adminProperties.isAdmin(authenticatedUser.email)) {
        throw ResponseStatusException(HttpStatus.FORBIDDEN, "admin access required")
    }
    return authenticatedUser
}
