package ru.itmo.market.kafka.consumer.implementation

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker
import io.github.resilience4j.retry.annotation.Retry
import mu.KotlinLogging
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.kafka.support.Acknowledgment
import org.springframework.kafka.support.KafkaHeaders
import org.springframework.messaging.handler.annotation.Header
import org.springframework.stereotype.Component
import ru.itmo.market.kafka.consumer.AbstractEventConsumer
import ru.itmo.market.kafka.consumer.EventConsumerState
import ru.itmo.market.kafka.event.order.OrderCreatedEvent
import ru.itmo.market.kafka.event.order.OrderPaidEvent
import ru.itmo.market.kafka.event.order.OrderDeliveredEvent
import ru.itmo.market.kafka.monitoring.KafkaMetricsCollector
import java.time.Instant
import java.time.Duration

@Component
class NotificationEventConsumer(
    private val notificationService: NotificationService,
    private val metricsCollector: KafkaMetricsCollector,
    private val consumerState: EventConsumerState
) : AbstractEventConsumer() {

    companion object {
        private val logger = KotlinLogging.logger {}
    }

    @KafkaListener(topics = ["order.created"], groupId = "notification-service-group")
    @CircuitBreaker(name = "notification-consumer", fallbackMethod = "handleFallback")
    @Retry(name = "notification-consumer")
    fun handleOrderCreatedNotification(
        event: OrderCreatedEvent,
        acknowledgment: Acknowledgment?,
        @Header(KafkaHeaders.RECEIVED_TOPIC) topic: String,
        @Header(KafkaHeaders.RECEIVED_PARTITION) partition: Int,
        @Header(KafkaHeaders.OFFSET) offset: Long
    ) {
        val startTime = Instant.now().toEpochMilli()
        try {
            logConsumption(event, topic, partition, offset)
            notificationService.sendOrderConfirmation(event)
            val processingTime = Instant.now().toEpochMilli() - startTime
            logSuccess(event, processingTime)
            consumerState.recordSuccess(event, processingTime)
            metricsCollector.recordEventConsumed("order.created.notification", processingTime)
            acknowledgment?.acknowledge()
        } catch (exception: Exception) {
            logError(event, exception, topic, partition, offset)
            consumerState.recordFailure(event, exception)
            acknowledgment?.nack(Duration.ofSeconds(3000))
            throw exception
        }
    }

    @KafkaListener(topics = ["order.paid"], groupId = "notification-service-group")
    @CircuitBreaker(name = "notification-consumer", fallbackMethod = "handleFallback")
    @Retry(name = "notification-consumer")
    fun handleOrderPaidNotification(
        event: OrderPaidEvent,
        acknowledgment: Acknowledgment?,
        @Header(KafkaHeaders.RECEIVED_TOPIC) topic: String,
        @Header(KafkaHeaders.RECEIVED_PARTITION) partition: Int,
        @Header(KafkaHeaders.OFFSET) offset: Long
    ) {
        val startTime = Instant.now().toEpochMilli()
        try {
            logConsumption(event, topic, partition, offset)
            notificationService.sendPaymentConfirmation(event)
            val processingTime = Instant.now().toEpochMilli() - startTime
            logSuccess(event, processingTime)
            consumerState.recordSuccess(event, processingTime)
            acknowledgment?.acknowledge()
        } catch (exception: Exception) {
            logError(event, exception, topic, partition, offset)
            consumerState.recordFailure(event, exception)
            acknowledgment?.nack(Duration.ofSeconds(3000))
            throw exception
        }
    }

    @KafkaListener(topics = ["order.delivered"], groupId = "notification-service-group")
    fun handleOrderDeliveredNotification(
        event: OrderDeliveredEvent,
        acknowledgment: Acknowledgment?,
        @Header(KafkaHeaders.RECEIVED_TOPIC) topic: String,
        @Header(KafkaHeaders.RECEIVED_PARTITION) partition: Int,
        @Header(KafkaHeaders.OFFSET) offset: Long
    ) {
        val startTime = Instant.now().toEpochMilli()
        try {
            logConsumption(event, topic, partition, offset)
            notificationService.sendDeliveryNotification(event)
            val processingTime = Instant.now().toEpochMilli() - startTime
            logSuccess(event, processingTime)
            consumerState.recordSuccess(event, processingTime)
            acknowledgment?.acknowledge()
        } catch (exception: Exception) {
            logError(event, exception, topic, partition, offset)
            consumerState.recordFailure(event, exception)
            acknowledgment?.nack(Duration.ofSeconds(3000))
            throw exception
        }
    }

    fun handleFallback(event: Any, acknowledgment: Acknowledgment?, exception: Exception) {
        logger.warn(exception) { "Circuit breaker opened for notification consumer" }
    }
}

interface NotificationService {
    fun sendOrderConfirmation(event: OrderCreatedEvent)
    fun sendPaymentConfirmation(event: OrderPaidEvent)
    fun sendDeliveryNotification(event: OrderDeliveredEvent)
}

@Component
class NotificationServiceImpl : NotificationService {
    companion object {
        private val logger = KotlinLogging.logger {}
    }

    override fun sendOrderConfirmation(event: OrderCreatedEvent) {
        logger.info { "Sending order confirmation email to: ${event.userEmail}" }
    }

    override fun sendPaymentConfirmation(event: OrderPaidEvent) {
        logger.info { "Sending payment confirmation to: ${event.userEmail}" }
    }

    override fun sendDeliveryNotification(event: OrderDeliveredEvent) {
        logger.info { "Sending delivery notification for order: ${event.orderId}" }
    }
}
