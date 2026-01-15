package ru.itmo.market.kafka.event.domain

import java.time.LocalDateTime
import java.util.UUID

/**
 * Value Object: Event timestamp with timezone awareness
 */
@JvmInline
value class EventTimestamp(val value: LocalDateTime = LocalDateTime.now()) {
    init {
        require(value <= LocalDateTime.now().plusSeconds(1)) { 
            "EventTimestamp cannot be in the future" 
        }
    }
}