package com.hwcompany.fortune_index.saju

import com.hwcompany.fortune_index.domain.model.SajuResult
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param

interface SajuResultRepository : JpaRepository<SajuResult, Long> {
    fun findTopByUserIdOrderByAnalyzedAtDesc(userId: Long): SajuResult?

    @Query(
        value = """
            select concat(stem.label_ko, branch.label_ko)
            from saju_results sr
            join saju_result_heavenly_stems stem on stem.saju_result_id = sr.id
            join saju_result_earthly_branches branch
                on branch.saju_result_id = sr.id
                and branch.pillar_order = stem.pillar_order
            where sr.id = (
                select latest.id
                from saju_results latest
                where latest.user_id = :userId
                order by latest.analyzed_at desc, latest.id desc
                limit 1
            )
            order by stem.pillar_order
        """,
        nativeQuery = true
    )
    fun findLatestPalzaPillarsByUserId(@Param("userId") userId: Long): List<String>

    @Query("select distinct sr.user.id from SajuResult sr where sr.user.id in :userIds")
    fun findExistingUserIds(userIds: Collection<Long>): List<Long>
}
