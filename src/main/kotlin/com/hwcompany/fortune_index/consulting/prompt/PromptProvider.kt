package com.hwcompany.fortune_index.consulting.prompt

import com.hwcompany.fortune_index.consulting.AnalysisMode

interface PromptProvider {
    fun supports(mode: AnalysisMode): Boolean

    fun systemMessage(): String
}
