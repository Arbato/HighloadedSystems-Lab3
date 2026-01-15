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
import ru.itmo.market.kafka.event.order.OrderCancelledEvent
import ru.itmo.market.kafka.monitoring.KafkaMetricsCollector
import java.time.Instant
import java.time.Duration

@Component
class ProductEventConsumer(
    private val productService: ProductService,
    private val metricsCollector: KafkaMetricsCollector,
    private val consumerState: EventConsumerState
) : AbstractEventConsumer() {

    companion object {
        private val logger = KotlinLogging.logger {}
    }

    @KafkaListener(topics = ["order.created"], groupId = "product-service-group")
    @CircuitBreaker(name = "product-consumer", fallbackMethod = "handleFallback")
    @Retry(name = "product-consumer")
    fun handleOrderCreatedReserveInventory(
        event: OrderCreatedEvent,
        acknowledgment: Acknowledgment?,
        @Header(KafkaHeaders.RECEIVED_TOPIC) topic: String,
        @Header(KafkaHeaders.RECEIVED_PARTITION) partition: Int,
        @Header(KafkaHeaders.OFFSET) offset: Long
    ) {
        val startTime = Instant.now().toEpochMilli()
        try {
            logConsumption(event, topic, partition, offset)
            productService.reserveInventory(event)
            val processingTime = Instant.now().toEpochMilli() - startTime
            logSuccess(event, processingTime)
            consumerState.recordSuccess(event, processingTime)
            metricsCollector.recordEventConsumed("order.created.product", processingTime)
            acknowledgment?.acknowledge()
        } catch (exception: Exception) {
            logError(event, exception, topic, partition, offset)
            consumerState.recordFailure(event, exception)
            acknowledgment?.nack(Duration.ofSeconds(3000))
            throw exception
        }
    }

    @KafkaListener(topics = ["order.paid"], groupId = "product-service-group")
    @CircuitBreaker(name = "product-consumer", fallbackMethod = "handleFallback")
    @Retry(name = "product-consumer")
    fun handleOrderPaidConfirmInventory(
        event: OrderPaidEvent,
        acknowledgment: Acknowledgment?,
        @Header(KafkaHeaders.RECEIVED_TOPIC) topic: String,
        @Header(KafkaHeaders.RECEIVED_PARTITION) partition: Int,
        @Header(KafkaHeaders.OFFSET) offset: Long
    ) {
        val startTime = Instant.now().toEpochMilli()
        try {
            logConsumption(event, topic, partition, offset)
            productService.confirmInventoryDeduction(event)
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

fun handleOrderCancelledFallback(
    event: OrderCancelledEvent,
    exception: Exception
) {
    logger.error(exception) {
        "Circuit breaker opened for order cancelled event: orderId=${event.orderId}"
    }
}


    fun handleFallback(event: Any, acknowledgment: Acknowledgment?, exception: Exception) {
        logger.warn(exception) { "Circuit breaker opened for product consumer" }
    }
}

interface ProductService {
    fun reserveInventory(event: OrderCreatedEvent)
    fun confirmInventoryDeduction(event: OrderPaidEvent)
}

@Component
class ProductServiceImpl : ProductService {
    companion object {
        private val logger = KotlinLogging.logger {}
    }

    override fun reserveInventory(event: OrderCreatedEvent) {
        logger.info { "Reserving inventory for order: ${event.orderId}" }
    }

    override fun confirmInventoryDeduction(event: OrderPaidEvent) {
        logger.info { "Confirming inventory deduction for order: ${event.orderId}" }
    }
}
