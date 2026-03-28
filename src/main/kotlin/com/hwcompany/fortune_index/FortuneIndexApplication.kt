package com.hwcompany.fortune_index

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.cloud.openfeign.EnableFeignClients

@SpringBootApplication
@EnableFeignClients
class FortuneIndexApplication

fun main(args: Array<String>) {
    runApplication<FortuneIndexApplication>(*args)
}
