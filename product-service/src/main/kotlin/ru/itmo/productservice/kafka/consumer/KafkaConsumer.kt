package ru.itmo.productservice.kafka.consumer

import org.slf4j.LoggerFactory
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.messaging.handler.annotation.Header
import org.springframework.messaging.handler.annotation.Payload
import org.springframework.stereotype.Component
import ru.itmo.productservice.client.KafkaRpcClient

@Component
class KafkaConsumer(
    private val kafkaRpcClient: KafkaRpcClient
) {
    private val logger = LoggerFactory.getLogger(javaClass)
    
    @KafkaListener(
        topics = ["product-service-replies"],
        groupId = "product-service-rpc-replies",
        containerFactory = "rpcReplyListenerContainerFactory"
    )
    fun handleUserServiceResponse(
        @Payload response: String,
        @Header("requestId") requestId: String
    ) {
        logger.debug("Received response from user-service: requestId=$requestId")
        kafkaRpcClient.handleResponse(response, requestId)
    }
}
