package ru.itmo.market.kafka.outbox

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

@Repository
interface OutboxRepository : JpaRepository<OutboxEvent, Long> {

    fun findByPublishedFalseOrderByCreatedAtAsc(): List<OutboxEvent>

    @Query(
        "SELECT oe FROM OutboxEvent oe WHERE oe.published = false " +
        "AND oe.attempts < :maxAttempts AND oe.updatedAt < :beforeTime " +
        "ORDER BY oe.createdAt ASC"
    )
    fun findFailedEvents(maxAttempts: Int, beforeTime: LocalDateTime): List<OutboxEvent>

    @Modifying
    @Transactional
    @Query("UPDATE OutboxEvent oe SET oe.published = true, oe.publishedAt = :time, oe.updatedAt = :time WHERE oe.id = :id")
    fun markAsPublished(id: Long, time: LocalDateTime)

    @Modifying
    @Transactional
    @Query("UPDATE OutboxEvent oe SET oe.attempts = oe.attempts + 1, oe.updatedAt = :time WHERE oe.id = :id")
    fun incrementAttempts(id: Long, time: LocalDateTime)

    @Query("SELECT oe FROM OutboxEvent oe WHERE oe.published = false AND oe.attempts >= :maxAttempts")
    fun findDeadLetterEvents(maxAttempts: Int): List<OutboxEvent>

    @Modifying
    @Transactional
    @Query("DELETE FROM OutboxEvent oe WHERE oe.published = true AND oe.publishedAt < :olderThan")
    fun deletePublishedOlderThan(olderThan: LocalDateTime): Int
}
