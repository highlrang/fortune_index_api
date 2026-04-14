package com.hwcompany.fortune_index.support

import com.hwcompany.fortune_index.domain.model.SupportInquiry
import com.hwcompany.fortune_index.domain.model.SupportInquiryStatus
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository

interface SupportInquiryRepository : JpaRepository<SupportInquiry, Long> {
    fun findAllByStatusOrderByCreatedAtDesc(status: SupportInquiryStatus, pageable: Pageable): List<SupportInquiry>

    fun findAllByOrderByCreatedAtDesc(pageable: Pageable): List<SupportInquiry>
}
