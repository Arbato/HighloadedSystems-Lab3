package ru.itmo.market.kafka.consumer

import mu.KotlinLogging
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.kafka.support.Acknowledgment
import org.springframework.stereotype.Component

@Component
class DeadLetterQueueHandler {
    companion object {
        private val logger = KotlinLogging.logger {}
    }

    @KafkaListener(
        topics = ["dlq.order.events", "dlq.product.events", "dlq.notification.events"],
        groupId = "dlq-handler-group"
    )
    fun handleDeadLetterEvent(payload: String, ack: Acknowledgment?) {
        logger.error { "Message in DLQ: $payload" }
        ack?.acknowledge()
    }
}
