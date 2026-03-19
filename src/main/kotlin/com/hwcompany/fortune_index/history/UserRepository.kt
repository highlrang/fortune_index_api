package com.hwcompany.fortune_index.history

import com.hwcompany.fortune_index.domain.model.User
import com.hwcompany.fortune_index.domain.model.UserAccountStatus
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository

interface UserRepository : JpaRepository<User, Long> {
    fun findByEmail(email: String): User?

    fun existsByEmail(email: String): Boolean

    fun findByAccountStatus(accountStatus: UserAccountStatus): List<User>

    fun findByIdGreaterThanOrderByIdAsc(id: Long, pageable: Pageable): List<User>
}
