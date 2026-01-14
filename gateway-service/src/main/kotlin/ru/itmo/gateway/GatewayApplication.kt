package ru.itmo.gateway

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.boot.runApplication
import org.springframework.cloud.client.discovery.EnableDiscoveryClient
import ru.itmo.gateway.config.JwtProperties

@SpringBootApplication
@EnableDiscoveryClient
@EnableConfigurationProperties(JwtProperties::class)
class GatewayApplication

fun main(args: Array<String>) {
    runApplication<GatewayApplication>(*args)
}
