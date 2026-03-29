package com.hwcompany.fortune_index.consulting

internal object MarketEvidencePromptGuidance {
    fun build(): String =
        buildString {
            append("응답은 반드시 최신 payload 안의 marketContext, marketPhenomenon, scenario, question만 근거로 작성해라. ")
            append("KIS 기반 시장 데이터는 추천 근거가 아니라 외부 공기의 결을 읽는 현상 지표다. ")
            append("market_analysis에는 observedAt 시점을 기준으로 바깥 분위기와 군중 심리를 짧게 요약하고, referenceSignal, sectorBias, tradingSignal, fundamentalSignal은 숫자 설명이 아니라 상징적 기류로 번역해라. ")
            append("숫자를 직접 나열하거나 가격 목표처럼 들리는 문장을 만들지 마라. ")
            append("데이터가 비어 있으면 없는 숫자를 지어내지 말고, 오늘의 공기를 읽을 단서가 제한적이라고 분명히 써라. ")
            append("사용자 상황을 한 문장으로 요약하며 공감부터 시작하되, 과장된 위로나 감정 과잉은 피하고 차분하게 지지해라. ")
            append("최종 결론은 행동 지시가 아니라 마음의 속도, 시선의 방향, 감정 관리 포인트를 제시하는 방식이어야 한다.")
        }
}
