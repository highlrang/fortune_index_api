package com.hwcompany.fortune_index

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.cloud.openfeign.EnableFeignClients
import org.springframework.scheduling.annotation.EnableScheduling

@SpringBootApplication
@EnableFeignClients
@EnableScheduling
class FortuneIndexApplication

fun main(args: Array<String>) {
    runApplication<FortuneIndexApplication>(*args)
}
