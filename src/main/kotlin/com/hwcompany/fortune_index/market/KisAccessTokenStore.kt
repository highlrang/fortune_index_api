package com.hwcompany.fortune_index.market

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import jakarta.persistence.Version
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Lock
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import java.time.LocalDateTime
import jakarta.persistence.LockModeType

@Entity
@Table(name = "market_access_tokens")
data class MarketAccessToken(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, unique = true, length = 30)
    var provider: MarketAccessTokenProvider,

    @Column(name = "access_token", nullable = false, length = 2000)
    var accessToken: String,

    @Column(name = "expires_at", nullable = false)
    var expiresAt: LocalDateTime,

    @Column(name = "issued_at", nullable = false)
    var issuedAt: LocalDateTime,

    @Column(name = "updated_at", nullable = false)
    var updatedAt: LocalDateTime = LocalDateTime.now(),

    @Version
    var version: Long? = null
)

enum class MarketAccessTokenProvider {
    KIS
}

@Repository
interface MarketAccessTokenRepository : JpaRepository<MarketAccessToken, Long> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select t from MarketAccessToken t where t.provider = :provider")
    fun findByProviderForUpdate(@Param("provider") provider: MarketAccessTokenProvider): MarketAccessToken?
}
