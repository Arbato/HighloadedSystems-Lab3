package ru.itmo.market.kafka.event.store

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import org.springframework.stereotype.Component
import mu.KotlinLogging

@Repository
interface EventStoreRepository : JpaRepository<StoredEvent, Long> {

    fun findByAggregateIdAndAggregateTypeOrderByVersionAsc(
        aggregateId: Long,
        aggregateType: String
    ): List<StoredEvent>

    @Query(
        "SELECT e FROM StoredEvent e WHERE e.aggregateId = :aggregateId " +
        "AND e.aggregateType = :aggregateType AND e.version > :fromVersion " +
        "ORDER BY e.version ASC"
    )
    fun getEventsSinceVersion(aggregateId: Long, aggregateType: String, fromVersion: Int): List<StoredEvent>

    fun findByEventTypeOrderByCreatedAtAsc(eventType: String): List<StoredEvent>
}
