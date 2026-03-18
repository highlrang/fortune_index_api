package com.hwcompany.fortune_index.scheduler

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "app.scheduler")
data class SchedulerProperties(
    var dailyFortune: DailyFortuneSchedulerProperties = DailyFortuneSchedulerProperties(),
    var dailyConsulting: DailyConsultingSchedulerProperties = DailyConsultingSchedulerProperties(),
    var hourlyRandomConsulting: HourlyRandomConsultingSchedulerProperties = HourlyRandomConsultingSchedulerProperties()
)

data class DailyFortuneSchedulerProperties(
    var enabled: Boolean = true,
    var cron: String = "0 0 9 * * *",
    var zone: String = "Asia/Seoul"
)

data class DailyConsultingSchedulerProperties(
    var enabled: Boolean = true,
    var zone: String = "Asia/Seoul",
    var onlyStockCron: String = "0 0 10 * * *",
    var stockSajuCron: String = "0 0 11 * * *",
    var stockTarotCron: String = "0 0 12 * * *",
    var stockAllCron: String = "0 0 13 * * *",
    var stockCandidates: List<String> = listOf(
        "005930",
        "000660",
        "035420",
        "051910",
        "068270",
        "105560",
        "035720",
        "012330"
    ),
    var questions: SchedulerQuestionProperties = SchedulerQuestionProperties()
)

data class HourlyRandomConsultingSchedulerProperties(
    var enabled: Boolean = false,
    var cron: String = "0 0 * * * *",
    var zone: String = "Asia/Seoul"
)

data class SchedulerQuestionProperties(
    var onlyStock: List<String> = listOf(
        "오늘 이 종목을 기술적 흐름 기준으로만 보면 어떻게 대응하는 게 좋을까?",
        "지금 시점에서 이 종목은 공격적으로 볼지 보수적으로 볼지 판단해줘.",
        "오늘 기준으로 진입, 관망, 정리 중 무엇이 가장 타당한지 말해줘."
    ),
    var stockSaju: List<String> = listOf(
        "오늘 이 종목 흐름이 내 사주 성향과 맞는지 같이 분석해줘.",
        "내 사주 기준으로 지금 이 종목에 접근하는 게 무리인지 봐줘.",
        "이 종목이 오늘 내 운의 흐름과 궁합이 맞는지 알려줘."
    ),
    var stockTarot: List<String> = listOf(
        "타로 흐름까지 포함하면 오늘 이 종목을 어떻게 해석하는 게 좋을까?",
        "이 종목에 대한 오늘의 심리 신호와 시장 신호를 함께 읽어줘.",
        "타로 기준까지 섞어서 지금 이 종목 대응 방향을 정리해줘."
    ),
    var stockAll: List<String> = listOf(
        "시장, 사주, 타로를 모두 반영하면 오늘 이 종목에 어떻게 대응해야 할까?",
        "오늘 이 종목은 종합적으로 진입, 보유, 정리 중 어디에 가까운지 알려줘.",
        "모든 신호를 합쳐서 오늘 이 종목의 투자 판단을 내려줘."
    )
)
