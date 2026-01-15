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
class OrderEventConsumer(
    private val orderService: OrderService,
    private val metricsCollector: KafkaMetricsCollector,
    private val consumerState: EventConsumerState
) : AbstractEventConsumer() {

    companion object {
        private val logger = KotlinLogging.logger {}
    }

    @KafkaListener(topics = ["order.created"], groupId = "order-service-group")
    @CircuitBreaker(name = "order-consumer", fallbackMethod = "handleFallback")
    @Retry(name = "order-consumer")
    fun handleOrderCreated(
        event: OrderCreatedEvent,
        acknowledgment: Acknowledgment?,
        @Header(KafkaHeaders.RECEIVED_TOPIC) topic: String,
        @Header(KafkaHeaders.RECEIVED_PARTITION) partition: Int,
        @Header(KafkaHeaders.OFFSET) offset: Long
    ) {
        val startTime = Instant.now().toEpochMilli()
        try {
            logConsumption(event, topic, partition, offset)
            orderService.processOrderCreated(event)

            val processingTime = Instant.now().toEpochMilli() - startTime
            logSuccess(event, processingTime)
            consumerState.recordSuccess(event, processingTime)
            metricsCollector.recordEventConsumed("order.created", processingTime)
            acknowledgment?.acknowledge()
        } catch (exception: Exception) {
            logError(event, exception, topic, partition, offset)
            consumerState.recordFailure(event, exception)
            acknowledgment?.nack(Duration.ofSeconds(3000))
            throw exception
        }
    }

    @KafkaListener(topics = ["order.paid"], groupId = "order-service-group")
    @CircuitBreaker(name = "order-consumer", fallbackMethod = "handleFallback")
    @Retry(name = "order-consumer")
    fun handleOrderPaid(
        event: OrderPaidEvent,
        acknowledgment: Acknowledgment?,
        @Header(KafkaHeaders.RECEIVED_TOPIC) topic: String,
        @Header(KafkaHeaders.RECEIVED_PARTITION) partition: Int,
        @Header(KafkaHeaders.OFFSET) offset: Long
    ) {
        val startTime = Instant.now().toEpochMilli()
        try {
            logConsumption(event, topic, partition, offset)
            orderService.processOrderPaid(event)
            val processingTime = Instant.now().toEpochMilli() - startTime
            logSuccess(event, processingTime)
            consumerState.recordSuccess(event, processingTime)
            metricsCollector.recordEventConsumed("order.paid", processingTime)
            acknowledgment?.acknowledge()
        } catch (exception: Exception) {
            logError(event, exception, topic, partition, offset)
            consumerState.recordFailure(event, exception)
            acknowledgment?.nack(Duration.ofSeconds(3000))
            throw exception
        }
    }

    @KafkaListener(topics = ["order.delivered"], groupId = "order-service-group")
    fun handleOrderDelivered(
        event: OrderDeliveredEvent,
        acknowledgment: Acknowledgment?,
        @Header(KafkaHeaders.RECEIVED_TOPIC) topic: String,
        @Header(KafkaHeaders.RECEIVED_PARTITION) partition: Int,
        @Header(KafkaHeaders.OFFSET) offset: Long
    ) {
        val startTime = Instant.now().toEpochMilli()
        try {
            logConsumption(event, topic, partition, offset)
            orderService.processOrderDelivered(event)
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
        logger.warn(exception) { "Circuit breaker opened for order consumer" }
    }
}

// Пустая реализация для примера
interface OrderService {
    fun processOrderCreated(event: OrderCreatedEvent)
    fun processOrderPaid(event: OrderPaidEvent)
    fun processOrderDelivered(event: OrderDeliveredEvent)
}

@Component
class OrderServiceImpl : OrderService {
    companion object {
        private val logger = KotlinLogging.logger {}
    }

    override fun processOrderCreated(event: OrderCreatedEvent) {
        logger.info { "Processing order created: ${event.orderId}" }
    }

    override fun processOrderPaid(event: OrderPaidEvent) {
        logger.info { "Processing order paid: ${event.orderId}" }
    }

    override fun processOrderDelivered(event: OrderDeliveredEvent) {
        logger.info { "Processing order delivered: ${event.orderId}" }
    }
}
