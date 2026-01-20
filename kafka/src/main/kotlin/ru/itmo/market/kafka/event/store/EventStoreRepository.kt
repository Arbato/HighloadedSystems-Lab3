package ru.itmo.market.kafka.event.store

import jakarta.persistence.*
import mu.KotlinLogging
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

@Repository
interface EventStoreRepository : JpaRepository<StoredEvent, Long> {
    fun findByEventId(eventId: String): StoredEvent?

    @Query(
        "SELECT e FROM StoredEvent e WHERE e.aggregateId = :aggregateId " +
        "AND e.aggregateType = :aggregateType ORDER BY e.version ASC"
    )
    fun findByAggregateIdAndAggregateType(aggregateId: Long, aggregateType: String): List<StoredEvent>

    @Query(
        "SELECT e FROM StoredEvent e WHERE e.aggregateId = :aggregateId " +
        "AND e.aggregateType = :aggregateType AND e.version >= :fromVersion " +
        "ORDER BY e.version ASC"
    )
    fun findByAggregateIdAndAggregateTypeFromVersion(
        aggregateId: Long,
        aggregateType: String,
        fromVersion: Int
    ): List<StoredEvent>

    @Query(
        "SELECT e FROM StoredEvent e WHERE e.eventType = :eventType " +
        "AND e.createdAt >= :since ORDER BY e.createdAt ASC"
    )
    fun findByEventTypeSince(eventType: String, since: LocalDateTime): List<StoredEvent>
}