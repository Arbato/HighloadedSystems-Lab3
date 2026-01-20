package ru.itmo.orderservice.adapter.out.messaging.kafka.consumer

import org.slf4j.LoggerFactory
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.messaging.handler.annotation.Header
import org.springframework.messaging.handler.annotation.Payload
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.stereotype.Component
import ru.itmo.orderservice.adapter.out.messaging.kafka.client.KafkaRpcClient

@Component
@ConditionalOnProperty(name = ["spring.kafka.enabled"], havingValue = "true", matchIfMissing = true)
class KafkaConsumer(
    private val kafkaRpcClient: KafkaRpcClient
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    @KafkaListener(
        topics = ["order-service-replies"],
        groupId = "order-service-rpc-replies"
    )
    fun handleProductServiceResponse(
        @Payload response: String,
        @Header("requestId") requestId: String
    ) {
        logger.debug("Received response from product-service: requestId=$requestId")
        kafkaRpcClient.handleResponse(response, requestId)
    }
}
