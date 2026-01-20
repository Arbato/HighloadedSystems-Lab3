package ru.itmo.market.kafka.idempotency

import jakarta.persistence.*
import mu.KotlinLogging
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

@Repository
interface ProcessedEventRepository : JpaRepository<ProcessedEvent, Long> {
    fun findByEventId(eventId: String): ProcessedEvent?

    @Query(
        "SELECT pe FROM ProcessedEvent pe WHERE pe.eventId = :eventId " +
        "AND pe.consumerGroup = :consumerGroup"
    )
    fun findByEventIdAndConsumerGroup(eventId: String, consumerGroup: String): ProcessedEvent?

    @Query(
        "DELETE FROM ProcessedEvent pe WHERE pe.processedAt < :before"
    )
    fun deleteOlderThan(before: LocalDateTime): Int
}