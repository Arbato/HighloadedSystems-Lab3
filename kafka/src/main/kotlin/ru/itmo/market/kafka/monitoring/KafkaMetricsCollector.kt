package ru.itmo.market.kafka.monitoring

import io.micrometer.core.instrument.*
import mu.KotlinLogging
import org.springframework.stereotype.Component
import java.util.concurrent.TimeUnit

@Component
class KafkaMetricsCollector(private val meterRegistry: MeterRegistry) {

    companion object {
        private val logger = KotlinLogging.logger {}
    }

    fun recordEventConsumed(eventType: String, durationMs: Long) {
        Timer.builder("kafka.event.consumed")
            .tag("event_type", eventType)
            .register(meterRegistry)
            .record(durationMs, TimeUnit.MILLISECONDS)
    }

    fun recordEventPublished(eventType: String) {
        Counter.builder("kafka.event.published")
            .tag("event_type", eventType)
            .register(meterRegistry)
            .increment()
    }

    fun recordEventConsumptionError(eventType: String, exception: Exception) {
        Counter.builder("kafka.event.error")
            .tag("event_type", eventType)
            .tag("error_type", exception.javaClass.simpleName)
            .register(meterRegistry)
            .increment()
    }

    fun recordDeadLetterEvent(topic: String, exception: Exception?) {
        Counter.builder("kafka.dead.letter")
            .tag("topic", topic)
            .tag("error_type", exception?.javaClass?.simpleName ?: "unknown")
            .register(meterRegistry)
            .increment()
        logger.warn { "DLQ Event: topic=$topic" }
    }

    fun recordCircuitBreakerEvent(consumerName: String, state: String) {
        Counter.builder("kafka.circuit.breaker")
            .tag("consumer", consumerName)
            .tag("state", state)
            .register(meterRegistry)
            .increment()
    }

    fun recordOutboxEventPublished(eventType: String, durationMs: Long) {
        Timer.builder("kafka.outbox.published")
            .tag("event_type", eventType)
            .register(meterRegistry)
            .record(durationMs, TimeUnit.MILLISECONDS)
    }
}
