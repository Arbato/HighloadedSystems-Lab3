package ru.itmo.market.kafka.event.store

import java.time.LocalDateTime
import jakarta.persistence.*

@Entity
@Table(name = "event_store")
data class StoredEvent(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,

    @Column(name = "event_type", nullable = false)
    val eventType: String,

    @Column(name = "aggregate_id", nullable = false)
    val aggregateId: Long,

    @Column(name = "aggregate_type", nullable = false)
    val aggregateType: String,

    @Column(name = "event_id", nullable = false, unique = true)
    val eventId: String,

    @Column(name = "version", nullable = false)
    val version: Int,

    @Column(name = "payload", nullable = false, columnDefinition = "TEXT")
    val payload: String,

    @Column(name = "correlation_id")
    val correlationId: String,

    @Column(name = "timestamp", nullable = false)
    val timestamp: LocalDateTime,

    @Column(name = "created_at", nullable = false, updatable = false)
    val createdAt: LocalDateTime = LocalDateTime.now()
)
