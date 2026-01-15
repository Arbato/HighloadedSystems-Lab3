package ru.itmo.market.kafka.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import ru.itmo.market.kafka.consumer.EventConsumerState
import ru.itmo.market.kafka.consumer.InMemoryEventConsumerState

/**
 * Configuration for event consumer state management
 */
@Configuration
class EventConsumerConfig {
    
    @Bean
    fun eventConsumerState(): EventConsumerState {
        return InMemoryEventConsumerState()
    }
}
