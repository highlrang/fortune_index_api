package com.hwcompany.fortune_index.common

import java.time.LocalDateTime
import java.time.ZoneId

object SeoulTime {
    val ZONE_ID: ZoneId = ZoneId.of("Asia/Seoul")

    fun now(): LocalDateTime = LocalDateTime.now(ZONE_ID)
}
