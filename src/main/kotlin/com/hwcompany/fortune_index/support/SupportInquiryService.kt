package com.hwcompany.fortune_index.support

import com.hwcompany.fortune_index.auth.AuthenticatedUser
import com.hwcompany.fortune_index.auth.MessageResponse
import com.hwcompany.fortune_index.domain.model.SupportInquiry
import com.hwcompany.fortune_index.domain.model.SupportInquiryStatus
import com.hwcompany.fortune_index.history.UserRepository
import org.slf4j.LoggerFactory
import org.springframework.data.domain.PageRequest
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.server.ResponseStatusException

@Service
class SupportInquiryService(
    private val userRepository: UserRepository,
    private val supportInquiryRepository: SupportInquiryRepository,
    private val supportInquiryNotifiers: List<SupportInquiryNotifier>
) {
    @Transactional
    fun createInquiry(authenticatedUser: AuthenticatedUser, request: CreateSupportInquiryRequest): MessageResponse {
        val user = userRepository.findById(authenticatedUser.userId)
            .orElseThrow { ResponseStatusException(HttpStatus.NOT_FOUND, "사용자를 찾을 수 없습니다: ${authenticatedUser.userId}") }

        val category = request.category
            ?: throw ResponseStatusException(HttpStatus.BAD_REQUEST, "문의 유형을 선택해주세요.")
        val content = request.content?.trim()
            ?: throw ResponseStatusException(HttpStatus.BAD_REQUEST, "문의 내용을 입력해주세요.")

        if (content.isBlank()) {
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "문의 내용을 입력해주세요.")
        }
        if (content.length > 1000) {
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "문의 내용은 1000자 이하여야 합니다.")
        }

        val inquiry = supportInquiryRepository.save(
            SupportInquiry(
                user = user,
                emailSnapshot = user.email,
                category = category,
                content = content
            )
        )

        supportInquiryNotifiers.forEach { notifier ->
            runCatching { notifier.notifyCreated(inquiry) }
                .onFailure {
                    logger.warn("failed to notify support inquiry creation. inquiryId={}", inquiry.id, it)
                }
        }

        return MessageResponse("문의가 접수되었습니다.")
    }

    @Transactional(readOnly = true)
    fun getRecentInquiriesForOperations(limit: Int = 50, status: SupportInquiryStatus? = null): List<SupportInquiry> {
        val pageable = PageRequest.of(0, limit.coerceIn(1, 200))
        return if (status == null) {
            supportInquiryRepository.findAllByOrderByCreatedAtDesc(pageable)
        } else {
            supportInquiryRepository.findAllByStatusOrderByCreatedAtDesc(status, pageable)
        }
    }

    companion object {
        private val logger = LoggerFactory.getLogger(SupportInquiryService::class.java)
    }
}
