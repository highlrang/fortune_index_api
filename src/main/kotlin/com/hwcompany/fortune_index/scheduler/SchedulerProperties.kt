package com.hwcompany.fortune_index.scheduler

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "app.scheduler")
data class SchedulerProperties(
    var enabled: Boolean = false,
    var dailyFortune: DailyFortuneSchedulerProperties = DailyFortuneSchedulerProperties(),
    var dailyConsulting: DailyConsultingSchedulerProperties = DailyConsultingSchedulerProperties(),
    var hourlyRandomConsulting: HourlyRandomConsultingSchedulerProperties = HourlyRandomConsultingSchedulerProperties()
)

data class DailyFortuneSchedulerProperties(
    var enabled: Boolean = true,
    var cron: String = "0 0 10 * * *",
    var zone: String = "Asia/Seoul"
)

data class DailyConsultingSchedulerProperties(
    var enabled: Boolean = true,
    var zone: String = "Asia/Seoul",
    var onlyStockCron: String = "0 5 10,16,22 * * *",
    var stockSajuCron: String = "0 10 10,16,22 * * *",
    var stockTarotCron: String = "0 15 10,16,22 * * *",
    var stockAllCron: String = "0 20 10,16,22 * * *",
    var representativeSectors: List<String> = listOf(
        "반도체",
        "2차전지",
        "바이오/헬스케어",
        "자동차",
        "인터넷/플랫폼",
        "게임/콘텐츠",
        "금융",
        "조선/방산",
        "화학",
        "에너지/전력인프라",
        "소비재/유통"
    )
)

data class HourlyRandomConsultingSchedulerProperties(
    var enabled: Boolean = false,
    var cron: String = "0 0 * * * *",
    var zone: String = "Asia/Seoul"
)
