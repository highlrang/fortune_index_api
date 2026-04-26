package com.hwcompany.fortune_index

import org.springframework.cache.annotation.EnableCaching
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import java.util.TimeZone

@SpringBootApplication
@EnableCaching
class FortuneIndexApplication

fun main(args: Array<String>) {
    TimeZone.setDefault(TimeZone.getTimeZone("Asia/Seoul"))
    runApplication<FortuneIndexApplication>(*args)
}
