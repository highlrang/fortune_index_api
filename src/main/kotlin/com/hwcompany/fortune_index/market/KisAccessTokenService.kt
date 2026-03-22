package com.hwcompany.fortune_index.market

import java.time.LocalDateTime
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class KisAccessTokenService(
    private val properties: StockMarketProperties,
    private val kisHeaderFactory: KisHeaderFactory,
    private val kisTokenFeignClient: KisTokenFeignClient,
    private val marketAccessTokenRepository: MarketAccessTokenRepository
) {
    @Transactional
    fun getAccessToken(): String {
        val now = LocalDateTime.now()
        val savedToken = marketAccessTokenRepository.findByProviderForUpdate(MarketAccessTokenProvider.KIS)

        if (savedToken != null && savedToken.expiresAt.isAfter(now.plusSeconds(properties.kis.tokenRefreshBufferSeconds))) {
            return savedToken.accessToken
        }

        val response = kisTokenFeignClient.issueAccessToken(
            headers = kisHeaderFactory.tokenHeaders(),
            request = KisTokenRequest(
                grant_type = "client_credentials",
                appkey = properties.kis.appKey,
                appsecret = properties.kis.appSecret
            )
        )

        val accessToken = response.normalizedAccessToken
            ?: throw IllegalStateException("KIS 접근 토큰이 없습니다.")
        val expiresIn = response.normalizedExpiresIn?.toLongOrNull() ?: DEFAULT_EXPIRES_IN_SECONDS
        val expiresAt = now.plusSeconds(expiresIn)

        marketAccessTokenRepository.save(
            (savedToken ?: MarketAccessToken(
                provider = MarketAccessTokenProvider.KIS,
                accessToken = accessToken,
                expiresAt = expiresAt,
                issuedAt = now
            )).apply {
                this.accessToken = accessToken
                this.expiresAt = expiresAt
                this.issuedAt = now
                this.updatedAt = now
            }
        )

        return accessToken
    }

    companion object {
        private const val DEFAULT_EXPIRES_IN_SECONDS = 86400L
    }
}
