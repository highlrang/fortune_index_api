package com.hwcompany.fortune_index.consulting

internal object MarketEvidencePromptGuidance {
    fun build(): String =
        buildString {
            append("응답은 반드시 최신 payload 안의 marketContext, marketPhenomenon, scenario, question만 근거로 작성해라. ")
            append("숫자 중심의 정보는 제거되었으므로 관심 분야 중심의 상징적 흐름만 사용해라. ")
            append("market_analysis에는 observedAt 시점을 기준으로 바깥 분위기를 짧게 요약하고, referenceSignal, flowBias, tradingSignal, fundamentalSignal은 어려운 설명 대신 쉬운 생활 언어로 풀어라. ")
            append("숫자를 길게 늘어놓거나 전문가처럼 단정하는 문장을 만들지 마라. ")
            append("데이터가 비어 있으면 없는 숫자를 지어내지 말고, 오늘의 흐름을 읽을 단서가 제한적이라고 분명히 써라. ")
            append("사용자 상황을 한 문장으로 요약하며 공감부터 시작하되, 과장된 위로나 감정 과잉은 피하고 차분하게 지지해라. ")
            append("최종 결론은 행동 지시가 아니라 마음의 속도, 시선의 방향, 감정 관리 포인트를 제시하는 방식이어야 한다.")
        }
}
