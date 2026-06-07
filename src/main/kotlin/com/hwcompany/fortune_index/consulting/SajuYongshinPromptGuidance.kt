package com.hwcompany.fortune_index.consulting

internal object SajuYongshinPromptGuidance {
    fun build(): String =
        "사주 해석 시 용신(用神)과 합충형파해(合沖刑破害)를 12운성·신살보다 우선 참고해라. " +
        "일간과 월지를 기준으로 신강·신약을 판단하고 용신·희신·기신을 분석해라. " +
        "investmentFeatures.dayMasterStrength(STRONG=신강/NEUTRAL=중화/WEAK=신약)는 참고 지표다. " +
        "용신은 나열하지 말고 해당 오행이 사주 균형에 미치는 역할로 설명해라. " +
        "합=연결·협력, 충=변화·이동, 형=압박·갈등, 파=균열·변동, 해=간접 방해·오해로 해석해라. " +
        "합을 무조건 길하게, 충을 무조건 흉하게 보지 마라. " +
        "investmentFeatures.relationSignals의 type: HAP=합, CHUNG=충, HYEONG=형, PA=파, HAE=해다. " +
        "target이 MAJOR_FLOW·YEARLY_FLOW면 운의 흐름, YEAR/MONTH/HOUR_NATAL이면 원국 내 관계다. " +
        "투자 성향·재물·리스크·의사결정 해석 시 용신·합충형파해 영향을 반영해라. " +
        "현재 대운·세운이 용신을 돕는지 약화시키는지, 재물·투자 변화 가능성을 함께 설명해라. " +
        "용신은 학파마다 해석이 다를 수 있으므로 절대 사실이 아닌 명리 해석 관점으로 전달해라."
}
