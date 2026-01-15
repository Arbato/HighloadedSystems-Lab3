package ru.itmo.market.kafka.config

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.KotlinModule
import org.springframework.beans.factory.annotation.Value
import org.springframework.kafka.core.KafkaTemplate
import io.micrometer.core.instrument.MeterRegistry
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.kafka.annotation.EnableKafka
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory
import org.springframework.kafka.core.ConsumerFactory
import org.springframework.kafka.core.DefaultKafkaConsumerFactory
import org.springframework.kafka.core.DefaultKafkaProducerFactory
import org.springframework.kafka.core.ProducerFactory
import org.springframework.kafka.listener.ContainerProperties
import org.springframework.kafka.support.KafkaHeaders
import org.springframework.kafka.support.serializer.ErrorHandlingDeserializer
import org.springframework.kafka.support.serializer.JsonDeserializer
import org.springframework.kafka.support.serializer.JsonSerializer
import org.springframework.messaging.handler.annotation.Header
import org.springframework.retry.annotation.EnableRetry
import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.clients.producer.ProducerConfig
import org.apache.kafka.common.serialization.StringDeserializer
import org.apache.kafka.common.serialization.StringSerializer
import io.micrometer.core.instrument.simple.SimpleMeterRegistry

/**
 * Kafka configuration for event-driven architecture
 * Handles producer and consumer configuration, serialization, and error handling
 */
@Configuration
@EnableKafka
@EnableRetry
class KafkaConfig(
    @Value("\${kafka.bootstrap-servers:localhost:9092}")
    private val bootstrapServers: String,

    @Value("\${kafka.consumer.group-id:market-service-group}")
    private val groupId: String,

    @Value("\${kafka.consumer.max-poll-records:100}")
    private val maxPollRecords: Int,

    @Value("\${kafka.consumer.session-timeout-ms:30000}")
    private val sessionTimeoutMs: Int,

    @Value("\${kafka.consumer.max-poll-interval-ms:300000}")
    private val maxPollIntervalMs: Int,

    @Value("\${kafka.consumer.auto-offset-reset:earliest}")
    private val autoOffsetReset: String,

    @Value("\${kafka.producer.acks:all}")
    private val acks: String,

    @Value("\${kafka.producer.retries:3}")
    private val retries: Int,

    @Value("\${kafka.producer.linger-ms:10}")
    private val lingerMs: Long,

    @Value("\${kafka.producer.batch-size:16384}")
    private val batchSize: Int,

    @Value("\${kafka.producer.compression-type:snappy}")
    private val compressionType: String
) {

    @Bean
    fun objectMapper(): ObjectMapper = ObjectMapper()
        .registerModule(KotlinModule.Builder().build())
        .registerModule(JavaTimeModule())
        .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)

    /**
     * Kafka Producer Factory Configuration
     * Configures serialization, acknowledgments, and retry behavior
     */
    @Bean
    fun producerFactory(objectMapper: ObjectMapper): ProducerFactory<String, Any> {
        val configs = mapOf(
            ProducerConfig.BOOTSTRAP_SERVERS_CONFIG to bootstrapServers,
            ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG to StringSerializer::class.java,
            ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG to JsonSerializer::class.java,
            ProducerConfig.ACKS_CONFIG to acks,  // "all" = leader + all replicas
            ProducerConfig.RETRIES_CONFIG to retries,
            ProducerConfig.LINGER_MS_CONFIG to lingerMs,  // Wait for batching
            ProducerConfig.BATCH_SIZE_CONFIG to batchSize,
            ProducerConfig.COMPRESSION_TYPE_CONFIG to compressionType,
            ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG to true,  // Exactly-once semantics
            ProducerConfig.MAX_IN_FLIGHT_REQUESTS_PER_CONNECTION to 5,
            ProducerConfig.REQUEST_TIMEOUT_MS_CONFIG to 30000,
            JsonSerializer.ADD_TYPE_INFO_HEADERS to false  // Disable type info headers
        )
        return DefaultKafkaProducerFactory(configs)
    }

    /**
     * Kafka Consumer Factory Configuration
     * Configures deserialization, offset management, and error handling
     */
    @Bean
    fun consumerFactory(objectMapper: ObjectMapper): ConsumerFactory<String, Any> {
        val configs = mapOf(
            ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG to bootstrapServers,
            ConsumerConfig.GROUP_ID_CONFIG to groupId,
            ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG to StringDeserializer::class.java,
            ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG to ErrorHandlingDeserializer::class.java,
            ErrorHandlingDeserializer.KEY_DESERIALIZER_CLASS to StringDeserializer::class.java.name,
            ErrorHandlingDeserializer.VALUE_DESERIALIZER_CLASS to JsonDeserializer::class.java.name,
            JsonDeserializer.VALUE_DEFAULT_TYPE to "ru.itmo.market.kafka.event.domain.DomainEvent",
            JsonDeserializer.TRUSTED_PACKAGES to "*",
            ConsumerConfig.AUTO_OFFSET_RESET_CONFIG to autoOffsetReset,
            ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG to false,  // Manual commit for safety
            ConsumerConfig.MAX_POLL_RECORDS_CONFIG to maxPollRecords,
            ConsumerConfig.SESSION_TIMEOUT_MS_CONFIG to sessionTimeoutMs,
            ConsumerConfig.MAX_POLL_INTERVAL_MS_CONFIG to maxPollIntervalMs,
            ConsumerConfig.HEARTBEAT_INTERVAL_MS_CONFIG to 3000,
            ConsumerConfig.ISOLATION_LEVEL_CONFIG to "read_committed"  // Read only committed messages
        )
        return DefaultKafkaConsumerFactory(configs)
    }

    /**
     * Concurrent Kafka Listener Container Factory
     * Configures listener behavior, concurrency, and error handling
     */
    @Bean
    fun kafkaListenerContainerFactory(
        consumerFactory: ConsumerFactory<String, Any>
    ): ConcurrentKafkaListenerContainerFactory<String, Any> {
        val factory = ConcurrentKafkaListenerContainerFactory<String, Any>()
        factory.consumerFactory = consumerFactory
        factory.setConcurrency(4)  // Number of consumer threads
        factory.setBatchListener(false)
        factory.containerProperties.ackMode = ContainerProperties.AckMode.MANUAL  // Manual acknowledgment
        factory.containerProperties.pollTimeout = 3000
        return factory
    }

    /**
     * Batch Kafka Listener Container Factory for batch processing
     */
    @Bean
    fun batchKafkaListenerContainerFactory(
        consumerFactory: ConsumerFactory<String, Any>
    ): ConcurrentKafkaListenerContainerFactory<String, Any> {
        val factory = ConcurrentKafkaListenerContainerFactory<String, Any>()
        factory.consumerFactory = consumerFactory
        factory.setConcurrency(2)
        factory.setBatchListener(true)
        factory.containerProperties.ackMode = ContainerProperties.AckMode.MANUAL_IMMEDIATE
        factory.containerProperties.pollTimeout = 5000
        return factory
    }

    @Bean
    fun kafkaTemplate(producerFactory: ProducerFactory<String, Any>): KafkaTemplate<String, Any> {
        return KafkaTemplate(producerFactory)
    }

    @Bean
    fun meterRegistry(): MeterRegistry {
        return SimpleMeterRegistry() 
    }
}