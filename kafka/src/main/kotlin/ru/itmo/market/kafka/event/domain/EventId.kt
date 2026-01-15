package ru.itmo.market.kafka.event.domain

import java.time.LocalDateTime
import java.util.UUID

@JvmInline
value class EventId(val value: String = UUID.randomUUID().toString()) {
    init {
        require(value.isNotBlank()) { "EventId cannot be blank" }
    }
    
    companion object {
        fun generate(): EventId = EventId(UUID.randomUUID().toString())
    }
}