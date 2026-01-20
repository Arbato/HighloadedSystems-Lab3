package ru.itmo.market.kafka.dlq

import jakarta.persistence.*
import mu.KotlinLogging
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.kafka.support.Acknowledgment
import org.springframework.kafka.support.KafkaHeaders
import org.springframework.messaging.handler.annotation.Header
import org.springframework.messaging.handler.annotation.Payload
import org.springframework.stereotype.Repository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

enum class DLQStatus {
    PENDING, RETRY, RESOLVED, ABANDONED, SKIPPED
}

@Entity
@Table(
    name = "dead_letter_queue",
    indexes = [
        Index(name = "idx_dlq_status", columnList = "status"),
        Index(name = "idx_dlq_created", columnList = "created_at"),
        Index(name = "idx_dlq_event_id", columnList = "event_id")
    ]
)
data class DeadLetterMessage(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,

    @Column(name = "event_id", nullable = false)
    val eventId: String,

    @Column(name = "event_type", nullable = false)
    val eventType: String,

    @Column(name = "topic", nullable = false)
    val topic: String,

    @Column(name = "partition", nullable = false)
    val partition: Int,

    @Column(name = "offset", nullable = false)
    val offset: Long,

    @Column(name = "payload", nullable = false, columnDefinition = "TEXT")
    val payload: String,

    @Column(name = "error_message", columnDefinition = "TEXT")
    val errorMessage: String?,

    @Column(name = "error_stack_trace", columnDefinition = "TEXT")
    val errorStackTrace: String?,

    @Column(name = "retry_count", nullable = false)
    var retryCount: Int = 0,

    @Column(name = "max_retries", nullable = false)
    val maxRetries: Int = 5,

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    var status: DLQStatus = DLQStatus.PENDING,

    @Column(name = "created_at", nullable = false, updatable = false)
    val createdAt: LocalDateTime = LocalDateTime.now(),

    @Column(name = "updated_at", nullable = false)
    var updatedAt: LocalDateTime = LocalDateTime.now(),

    @Column(name = "resolved_at")
    var resolvedAt: LocalDateTime? = null
)