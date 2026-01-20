package ru.itmo.market.kafka.event.store

import jakarta.persistence.*
import mu.KotlinLogging
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

@Service
class EventStoreService(
    private val eventStoreRepository: EventStoreRepository
) {
    private val logger = KotlinLogging.logger { }

    @Transactional
    fun storeEvent(event: StoredEvent): StoredEvent {
        try {
            val existingEvent = eventStoreRepository.findByEventId(event.eventId)
            if (existingEvent != null) {
                logger.warn { "Event already exists: ${event.eventId}" }
                return existingEvent
            }

            val savedEvent = eventStoreRepository.save(event)
            logger.info {
                "Event stored. ID: ${event.eventId}, Type: ${event.eventType}, " +
                "Aggregate: ${event.aggregateId}"
            }
            return savedEvent
        } catch (e: Exception) {
            logger.error(e) { "Error storing event: ${event.eventId}" }
            throw e
        }
    }

    fun getEventById(eventId: String): StoredEvent? {
        return eventStoreRepository.findByEventId(eventId)
    }

    fun getAggregateEvents(aggregateId: Long, aggregateType: String): List<StoredEvent> {
        return eventStoreRepository.findByAggregateIdAndAggregateType(aggregateId, aggregateType)
    }

    fun getAggregateEventsFromVersion(
        aggregateId: Long,
        aggregateType: String,
        fromVersion: Int
    ): List<StoredEvent> {
        return eventStoreRepository.findByAggregateIdAndAggregateTypeFromVersion(
            aggregateId, aggregateType, fromVersion
        )
    }

    fun getEventsByTypeSince(eventType: String, since: LocalDateTime): List<StoredEvent> {
        return eventStoreRepository.findByEventTypeSince(eventType, since)
    }

    fun replayAggregateEvents(aggregateId: Long, aggregateType: String): List<StoredEvent> {
        return getAggregateEvents(aggregateId, aggregateType).sortedBy { it.version }
    }
}