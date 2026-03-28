package com.hwcompany.fortune_index.consulting

internal object MarketEvidencePromptGuidance {
    fun build(): String =
        buildString {
            append("응답은 반드시 최신 payload 안의 marketContext, internalStockData, scenario, question만 근거로 작성해라. ")
            append("market_analysis에는 marketDataAsOf 날짜를 기준으로 현재 시장 서사를 먼저 짧게 요약하고, referenceSignal, sectorBias, tradingSignal, fundamentalSignal 중 확인 가능한 항목을 근거로 붙여라. ")
            append("펀더멘털 수치가 있으면 그 숫자가 뜻하는 바를 초등학생도 이해할 수 있을 만큼 쉽게 설명해라. ")
            append("펀더멘털 수치가 비어 있으면 없는 숫자를 지어내지 말고, 확보된 지표가 제한적이라 가격 흐름 해석 비중이 높다고 분명히 써라. ")
            append("사용자 상황을 한 문장으로 요약하며 공감부터 시작하되, 과장된 위로나 감정 과잉은 피하고 차분하게 지지해라. ")
            append("최종 결론은 전량 매수나 전량 매도 같은 단정 대신 분할매수, 비중 유지, 현금 확보 같은 전략적 선택지로 제시해라.")
        }
}
