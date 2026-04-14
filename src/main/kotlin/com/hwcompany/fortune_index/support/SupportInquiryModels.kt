package com.hwcompany.fortune_index.support

import com.hwcompany.fortune_index.domain.model.SupportInquiryCategory

data class CreateSupportInquiryRequest(
    val category: SupportInquiryCategory?,
    val content: String?
)
