package com.hwcompany.fortune_index

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.cloud.openfeign.EnableFeignClients
import java.util.TimeZone

@SpringBootApplication
@EnableFeignClients
class FortuneIndexApplication

fun main(args: Array<String>) {
    TimeZone.setDefault(TimeZone.getTimeZone("Asia/Seoul"))
    runApplication<FortuneIndexApplication>(*args)
}
