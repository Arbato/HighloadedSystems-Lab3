package ru.itmo.fileservice

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.cloud.openfeign.EnableFeignClients
import org.springframework.cache.annotation.EnableCaching
import org.springframework.scheduling.annotation.EnableAsync
import org.springframework.context.annotation.ComponentScan

@SpringBootApplication
@EnableFeignClients(basePackages = ["ru.itmo.fileservice.client"])
@EnableCaching
@EnableAsync
@ComponentScan(basePackages = ["ru.itmo.fileservice"])
class FileServiceApplication

fun main(args: Array<String>) {
    runApplication<FileServiceApplication>(*args)
}
