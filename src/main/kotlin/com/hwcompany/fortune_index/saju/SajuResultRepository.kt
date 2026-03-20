package com.hwcompany.fortune_index.saju

import com.hwcompany.fortune_index.domain.model.SajuResult
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query

interface SajuResultRepository : JpaRepository<SajuResult, Long> {
    fun findTopByUserIdOrderByAnalyzedAtDesc(userId: Long): SajuResult?

    @Query("select distinct sr.user.id from SajuResult sr where sr.user.id in :userIds")
    fun findExistingUserIds(userIds: Collection<Long>): List<Long>
}
