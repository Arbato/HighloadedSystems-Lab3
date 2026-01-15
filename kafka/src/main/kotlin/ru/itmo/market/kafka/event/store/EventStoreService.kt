package ru.itmo.market.kafka.event.store

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import org.springframework.stereotype.Component
import mu.KotlinLogging

@Component
class EventStoreService(
    private val repository: EventStoreRepository
) {
    companion object {
        private val logger = KotlinLogging.logger {}
    }

    fun storeEvent(event: StoredEvent) {
        repository.save(event)
        logger.debug { "Event stored: ${event.eventType}" }
    }

    fun getAggregateHistory(aggregateId: Long, aggregateType: String): List<StoredEvent> {
        return repository.findByAggregateIdAndAggregateTypeOrderByVersionAsc(aggregateId, aggregateType)
    }
}
