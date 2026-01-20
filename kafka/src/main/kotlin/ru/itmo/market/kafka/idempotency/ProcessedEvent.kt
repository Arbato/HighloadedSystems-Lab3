package ru.itmo.market.kafka.idempotency

import jakarta.persistence.*
import mu.KotlinLogging
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

@Entity
@Table(
    name = "processed_events",
    indexes = [
        Index(name = "idx_processed_event_id", columnList = "event_id", unique = true),
        Index(name = "idx_processed_type", columnList = "event_type"),
        Index(name = "idx_processed_created", columnList = "processed_at")
    ]
)
data class ProcessedEvent(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,

    @Column(name = "event_id", nullable = false, unique = true)
    val eventId: String,

    @Column(name = "event_type", nullable = false)
    val eventType: String,

    @Column(name = "aggregate_id", nullable = false)
    val aggregateId: Long,

    @Column(name = "aggregate_type", nullable = false)
    val aggregateType: String,

    @Column(name = "consumer_group", nullable = false)
    val consumerGroup: String,

    @Column(name = "processed_at", nullable = false, updatable = false)
    val processedAt: LocalDateTime = LocalDateTime.now(),

    @Column(name = "processing_time_ms", nullable = false)
    val processingTimeMs: Long = 0
)