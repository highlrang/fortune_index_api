package com.hwcompany.fortune_index.support

import com.hwcompany.fortune_index.domain.model.SupportInquiry
import org.springframework.stereotype.Component

fun interface SupportInquiryNotifier {
    fun notifyCreated(inquiry: SupportInquiry)
}

@Component
class NoOpSupportInquiryNotifier : SupportInquiryNotifier {
    override fun notifyCreated(inquiry: SupportInquiry) = Unit
}
