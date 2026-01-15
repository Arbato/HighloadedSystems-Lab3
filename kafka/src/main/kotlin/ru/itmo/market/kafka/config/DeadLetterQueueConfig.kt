package ru.itmo.market.kafka.config

import org.apache.kafka.clients.admin.NewTopic
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class DeadLetterQueueConfig {

    @Bean
    fun dlqOrderTopic(): NewTopic = NewTopic("dlq.order.events", 3, 1)
        .configs(mapOf("retention.ms" to "2592000000", "compression.type" to "snappy"))

    @Bean
    fun dlqProductTopic(): NewTopic = NewTopic("dlq.product.events", 3, 1)
        .configs(mapOf("retention.ms" to "2592000000", "compression.type" to "snappy"))

    @Bean
    fun dlqNotificationTopic(): NewTopic = NewTopic("dlq.notification.events", 3, 1)
        .configs(mapOf("retention.ms" to "2592000000", "compression.type" to "snappy"))

    @Bean
    fun dlqOutboxTopic(): NewTopic = NewTopic("dlq.outbox.events", 3, 1)
        .configs(mapOf("retention.ms" to "2592000000", "compression.type" to "snappy"))
}
