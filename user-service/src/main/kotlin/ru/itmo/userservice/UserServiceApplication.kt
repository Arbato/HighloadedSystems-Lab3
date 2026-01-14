package ru.itmo.userservice

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.boot.runApplication
import org.springframework.cloud.client.discovery.EnableDiscoveryClient
import ru.itmo.userservice.config.JwtProperties

@SpringBootApplication
@EnableDiscoveryClient
@EnableConfigurationProperties(JwtProperties::class)
class UserServiceApplication

fun main(args: Array<String>) {
    runApplication<UserServiceApplication>(*args)
}
