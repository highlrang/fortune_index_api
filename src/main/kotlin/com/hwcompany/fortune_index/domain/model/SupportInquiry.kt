package com.hwcompany.fortune_index.domain.model

import com.hwcompany.fortune_index.common.SeoulTime
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Index
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.PrePersist
import jakarta.persistence.PreUpdate
import jakarta.persistence.Table
import java.time.LocalDateTime

@Entity
@Table(
    name = "support_inquiries",
    indexes = [
        Index(name = "idx_support_inquiries_user_id", columnList = "user_id"),
        Index(name = "idx_support_inquiries_status", columnList = "status"),
        Index(name = "idx_support_inquiries_created_at", columnList = "created_at")
    ]
)
data class SupportInquiry(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    var user: User,

    @Column(name = "email_snapshot", nullable = false, length = 120)
    var emailSnapshot: String,

    @Enumerated(EnumType.STRING)
    @Column(name = "category", nullable = false, length = 20)
    var category: SupportInquiryCategory,

    @Column(name = "content", nullable = false, length = 1000)
    var content: String,

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    var status: SupportInquiryStatus = SupportInquiryStatus.RECEIVED,

    @Column(name = "created_at", nullable = false)
    var createdAt: LocalDateTime = SeoulTime.now(),

    @Column(name = "updated_at", nullable = false)
    var updatedAt: LocalDateTime = SeoulTime.now()
) {
    @PrePersist
    fun onCreate() {
        val now = SeoulTime.now()
        createdAt = now
        updatedAt = now
    }

    @PreUpdate
    fun onUpdate() {
        updatedAt = SeoulTime.now()
    }
}

enum class SupportInquiryCategory {
    SERVICE,
    BILLING,
    TECHNICAL,
    OTHER
}

enum class SupportInquiryStatus {
    RECEIVED,
    IN_PROGRESS,
    CLOSED
}
