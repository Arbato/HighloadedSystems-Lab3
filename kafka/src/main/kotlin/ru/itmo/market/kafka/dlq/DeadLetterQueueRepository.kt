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

@Repository
interface DeadLetterQueueRepository : JpaRepository<DeadLetterMessage, Long> {
    fun findByStatus(status: DLQStatus): List<DeadLetterMessage>

    @Query(
        "SELECT d FROM DeadLetterMessage d WHERE d.status = :status AND d.retryCount < d.maxRetries " +
        "ORDER BY d.createdAt ASC"
    )
    fun findRetryableMessages(status: DLQStatus): List<DeadLetterMessage>

    fun findByEventId(eventId: String): DeadLetterMessage?
}