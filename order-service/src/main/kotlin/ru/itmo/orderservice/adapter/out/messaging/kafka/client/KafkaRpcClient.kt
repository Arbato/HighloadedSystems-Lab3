package ru.itmo.orderservice.adapter.out.messaging.kafka.client

import com.fasterxml.jackson.databind.ObjectMapper
import org.slf4j.LoggerFactory
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.kafka.support.KafkaHeaders
import org.springframework.messaging.Message
import org.springframework.messaging.support.MessageBuilder
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.stereotype.Component
import ru.itmo.orderservice.adapter.out.messaging.kafka.client.ProductServiceRequest
import ru.itmo.orderservice.adapter.out.messaging.kafka.client.ProductServiceResponse
import ru.itmo.orderservice.adapter.out.messaging.kafka.client.ProductData
import ru.itmo.orderservice.domain.model.dto.response.ProductResponse
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

@Component
@ConditionalOnProperty(name = ["spring.kafka.enabled"], havingValue = "true", matchIfMissing = true)
class KafkaRpcClient(
    private val kafkaTemplate: KafkaTemplate<String, String>,
    private val objectMapper: ObjectMapper
) {
    private val logger = LoggerFactory.getLogger(javaClass)
    private val pendingRequests: ConcurrentHashMap<String, PendingRequest> = ConcurrentHashMap()

    companion object {
        private const val PRODUCT_SERVICE_REQUEST_TOPIC = "product-service-requests"
        private const val ORDER_SERVICE_REPLY_TOPIC = "order-service-replies"
        private const val REQUEST_TIMEOUT_SECONDS = 10L
    }

    fun getProductById(productId: Long): ProductResponse {
        val requestId = UUID.randomUUID().toString()
        logger.info("Requesting product with ID $productId (requestId: $requestId)")

        val request = ProductServiceRequest(
            requestId = requestId,
            requestType = "GET_PRODUCT_BY_ID",
            productId = productId
        )

        val response = sendRequest(requestId, request)

        val product = response.product
            ?: throw RuntimeException("Product not found: $productId")

        return ProductResponse(
            id = product.id,
            name = product.name,
            description = product.description,
            price = product.price,
            imageUrl = product.imageUrl,
            shopId = product.shopId,
            sellerId = product.sellerId,
            status = product.status,
            rejectionReason = product.rejectionReason,
            createdAt = java.time.LocalDateTime.now(),
            updatedAt = java.time.LocalDateTime.now()
        )
    }

    private fun sendRequest(requestId: String, request: ProductServiceRequest): ProductServiceResponse {
        val latch = CountDownLatch(1)
        val pendingRequest = PendingRequest(latch = latch)
        pendingRequests[requestId] = pendingRequest

        try {
            val requestJson = objectMapper.writeValueAsString(request)

            val message: Message<String> = MessageBuilder
                .withPayload(requestJson)
                .setHeader(KafkaHeaders.TOPIC, PRODUCT_SERVICE_REQUEST_TOPIC)
                .setHeader("requestId", requestId)
                .setHeader("replyTopic", ORDER_SERVICE_REPLY_TOPIC)
                .build()

            kafkaTemplate.send(message)
            logger.debug("Request sent to Kafka (requestId: $requestId)")

            val received = latch.await(REQUEST_TIMEOUT_SECONDS, TimeUnit.SECONDS)

            if (!received) {
                logger.error("Request timeout after $REQUEST_TIMEOUT_SECONDS seconds (requestId: $requestId)")
                throw RuntimeException("Product service request timeout")
            }

            val response = pendingRequest.response
                ?: throw RuntimeException("No response received from product service")

            if (!response.success) {
                logger.warn("Product service returned error: ${response.errorMessage}")
                throw RuntimeException("Product service error: ${response.errorMessage}")
            }

            logger.info("Received response for requestId: $requestId")
            return response

        } finally {
            pendingRequests.remove(requestId)
        }
    }

    fun handleResponse(responseJson: String, requestId: String) {
        try {
            logger.debug("Handling response for requestId: $requestId")
            val response = objectMapper.readValue(responseJson, ProductServiceResponse::class.java)

            val pendingRequest = pendingRequests[requestId]
            if (pendingRequest != null) {
                pendingRequest.response = response
                pendingRequest.latch.countDown()
            } else {
                logger.warn("Received response for unknown requestId: $requestId")
            }
        } catch (e: Exception) {
            logger.error("Error processing response for requestId: $requestId", e)
            val pendingRequest = pendingRequests[requestId]
            if (pendingRequest != null) {
                pendingRequest.latch.countDown()
            }
        }
    }

    private data class PendingRequest(
        val latch: CountDownLatch,
        var response: ProductServiceResponse? = null
    )
}
