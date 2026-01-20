package ru.itmo.orderservice.kafka.consumer

import com.fasterxml.jackson.databind.ObjectMapper
import org.slf4j.LoggerFactory
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.messaging.handler.annotation.Header
import org.springframework.messaging.handler.annotation.Payload
import org.springframework.stereotype.Component
import ru.itmo.orderservice.client.KafkaRpcClient

@Component
class OrderServiceReplyListener(
    private val kafkaRpcClient: KafkaRpcClient,
    private val objectMapper: ObjectMapper
) {
    private val logger = LoggerFactory.getLogger(javaClass)
    
    @KafkaListener(topics = ["order-service-replies"])
    fun handleOrderServiceReplies(
        @Payload responseJson: String,
        @Header("requestId") requestId: String
    ) {
        try {
            logger.debug("Received reply for requestId: $requestId, payload: $responseJson")
            kafkaRpcClient.handleResponse(responseJson, requestId)
            logger.info("Successfully processed reply for requestId: $requestId")
        } catch (e: Exception) {
            logger.error("Error handling reply for requestId: $requestId", e)
        }
    }
}