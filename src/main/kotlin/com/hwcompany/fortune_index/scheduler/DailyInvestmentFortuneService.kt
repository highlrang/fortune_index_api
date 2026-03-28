package com.hwcompany.fortune_index.scheduler

import com.hwcompany.fortune_index.domain.model.DailyInvestmentFortune
import com.hwcompany.fortune_index.investmentindex.InvestmentIndexService
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZonedDateTime
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class DailyInvestmentFortuneService(
    private val dailyInvestmentFortuneRepository: DailyInvestmentFortuneRepository,
    private val investmentIndexService: InvestmentIndexService,
    private val schedulerProperties: SchedulerProperties
) {
    @Transactional
    fun saveTodayFortuneIfAbsent(): DailyInvestmentFortune? {
        val zoneId = java.time.ZoneId.of(schedulerProperties.dailyFortune.zone)
        val now = ZonedDateTime.now(zoneId)
        val today = now.toLocalDate()

        if (dailyInvestmentFortuneRepository.existsByFortuneDate(today)) {
            logger.info("Daily investment fortune already exists for date={}", today)
            return null
        }

        val index = investmentIndexService.getInvestmentIndex(now)
        val saved = dailyInvestmentFortuneRepository.save(
            DailyInvestmentFortune(
                fortuneDate = today,
                totalScore = index.totalScore,
                selectedMarket = index.detail.selectedMarket,
                marketScore = index.detail.marketScore,
                marketRawValue = BigDecimal.valueOf(index.detail.marketRawValue),
                sajuScore = index.detail.sajuScore,
                dailyGanji = index.detail.dailyGanji,
                tarotScore = index.detail.tarotScore,
                tarotCardName = index.detail.tarotCardName,
                createdAt = LocalDateTime.ofInstant(now.toInstant(), zoneId)
            )
        )

        logger.info("Saved daily investment fortune for date={} score={}", today, saved.totalScore)
        return saved
    }

    @Transactional(readOnly = true)
    fun existsByDate(date: LocalDate): Boolean = dailyInvestmentFortuneRepository.existsByFortuneDate(date)

    private companion object {
        private val logger = LoggerFactory.getLogger(DailyInvestmentFortuneService::class.java)
    }
}
