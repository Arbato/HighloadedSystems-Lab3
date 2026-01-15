package ru.itmo.market.kafka.outbox

import java.time.LocalDateTime
import jakarta.persistence.*

@Entity
@Table(name = "outbox")
data class OutboxEvent(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,

    @Column(name = "event_type", nullable = false)
    val eventType: String,

    @Column(name = "aggregate_id", nullable = false)
    val aggregateId: Long,

    @Column(name = "aggregate_type", nullable = false)
    val aggregateType: String,

    @Column(name = "payload", nullable = false, columnDefinition = "TEXT")
    val payload: String,

    @Column(name = "correlation_id", nullable = false)
    val correlationId: String,

    @Column(name = "published", nullable = false)
    var published: Boolean = false,

    @Column(name = "published_at")
    var publishedAt: LocalDateTime? = null,

    @Column(name = "attempts", nullable = false)
    var attempts: Int = 0,

    @Column(name = "created_at", nullable = false, updatable = false)
    val createdAt: LocalDateTime = LocalDateTime.now(),

    @Column(name = "updated_at")
    var updatedAt: LocalDateTime = LocalDateTime.now()
)
