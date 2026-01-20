package ru.itmo.orderservice.config

import org.mockito.Mockito
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Primary
import ru.itmo.orderservice.kafka.client.KafkaRpcClient

@Configuration
@ConditionalOnProperty(name = ["spring.kafka.enabled"], havingValue = "false")
class TestKafkaConfig {

    @Bean
    @Primary
    fun kafkaRpcClient(): KafkaRpcClient {
        return Mockito.mock(KafkaRpcClient::class.java)
    }
}
