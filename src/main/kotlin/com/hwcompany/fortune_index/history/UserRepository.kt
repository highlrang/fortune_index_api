package com.hwcompany.fortune_index.history

import com.hwcompany.fortune_index.domain.model.User
import org.springframework.data.jpa.repository.JpaRepository

interface UserRepository : JpaRepository<User, Long>
