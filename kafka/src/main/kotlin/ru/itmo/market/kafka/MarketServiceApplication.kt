package ru.itmo.market.kafka

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.cloud.client.discovery.EnableDiscoveryClient
import org.springframework.scheduling.annotation.EnableScheduling
import org.springframework.retry.annotation.EnableRetry
import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity
import org.springframework.transaction.annotation.EnableTransactionManagement

/**
 * Market Service - Main Spring Boot Application
 * Implements Kafka Outbox Pattern with Event Sourcing
 *
 * Features:
 * - Event-driven architecture with Kafka
 * - Outbox pattern for reliable event publishing
 * - Event sourcing for audit trail
 * - Circuit breaker and retry mechanisms
 * - Distributed transaction handling
 * - Dead letter queue processing
 * - Idempotent message processing
 */
@SpringBootApplication
@EnableDiscoveryClient
@EnableScheduling
@EnableRetry
@EnableTransactionManagement
@EnableGlobalMethodSecurity(prePostEnabled = true)
class MarketServiceApplication

fun main(args: Array<String>) {
    runApplication<MarketServiceApplication>(*args)
}