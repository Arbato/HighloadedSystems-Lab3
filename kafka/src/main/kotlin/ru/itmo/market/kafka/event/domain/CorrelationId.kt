package ru.itmo.market.kafka.event.domain

import java.time.LocalDateTime
import java.util.UUID

/**
 * Value Object: Correlation ID for distributed tracing
 */
@JvmInline
value class CorrelationId(val value: String = UUID.randomUUID().toString()) {
    init {
        require(value.isNotBlank()) { "CorrelationId cannot be blank" }
    }
    
    companion object {
        fun generate(): CorrelationId = CorrelationId(UUID.randomUUID().toString())
    }
}