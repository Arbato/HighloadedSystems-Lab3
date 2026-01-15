package ru.itmo.market.kafka.event.domain

import java.time.LocalDateTime
import java.util.UUID

/**
 * Base aggregate identifier (can be extended by specific services)
 */
open class AggregateId(open val value: Long) {
    init {
        require(value > 0) { "AggregateId must be positive" }
    }
    
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is AggregateId) return false
        return value == other.value
    }
    
    override fun hashCode(): Int = value.hashCode()
    override fun toString(): String = value.toString()
}