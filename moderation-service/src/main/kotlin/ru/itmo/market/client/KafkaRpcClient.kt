package ru.itmo.market.client

import com.fasterxml.jackson.databind.ObjectMapper
import org.slf4j.LoggerFactory
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.kafka.support.KafkaHeaders
import org.springframework.messaging.support.MessageBuilder
import org.springframework.stereotype.Component
import ru.itmo.market.exception.ResourceNotFoundException
import ru.itmo.market.model.dto.kafka.ProductServiceResponse
import ru.itmo.market.model.dto.kafka.GetProductsPayload
import ru.itmo.market.model.dto.kafka.GetProductPayload
import ru.itmo.market.model.dto.kafka.ApproveProductPayload
import ru.itmo.market.model.dto.kafka.RejectProductPayload
import ru.itmo.market.model.dto.kafka.UserServiceResponse
import ru.itmo.market.model.dto.kafka.ProductServiceRequest
import ru.itmo.market.model.dto.kafka.UserServiceRequest
import ru.itmo.market.model.dto.kafka.GetUserPayload
import ru.itmo.market.model.dto.response.ProductResponse
import ru.itmo.market.model.dto.response.PaginatedResponse
import ru.itmo.market.model.dto.response.UserResponse
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import com.fasterxml.jackson.core.type.TypeReference


@Component
class KafkaRpcClient(
    private val kafkaTemplate: KafkaTemplate<String, String>,
    private val objectMapper: ObjectMapper
) {
    private val logger = LoggerFactory.getLogger(javaClass)
    private val pendingRequests: ConcurrentHashMap<String, PendingRequest> = ConcurrentHashMap()
    
    companion object {
        private const val PRODUCT_SERVICE_REQUEST_TOPIC = "product-service-requests"
        private const val USER_SERVICE_REQUEST_TOPIC = "user-service-requests"
        private const val MODERATION_SERVICE_REPLY_TOPIC = "moderation-service-replies"
        private const val REQUEST_TIMEOUT_SECONDS = 10L
    }
    
    fun <T> sendRequest(
        requestTopic: String,
        replyTopic: String,
        request: Any,
        responseType: TypeReference<T>  // ← TypeReference вместо Class
    ): T {
        val requestId = UUID.randomUUID().toString()
        logger.info("Sending RPC request: requestId=$requestId, topic=$requestTopic")
        
        val latch = CountDownLatch(1)
        val pendingRequest = PendingRequest(latch = latch)
        pendingRequests[requestId] = pendingRequest
        
        try {
            val requestJson = objectMapper.writeValueAsString(request)
            
            val message = MessageBuilder
                .withPayload(requestJson)
                .setHeader(KafkaHeaders.TOPIC, requestTopic)
                .setHeader("requestId", requestId)
                .setHeader("replyTopic", replyTopic)
                .build()
            
            kafkaTemplate.send(message)
            logger.debug("Request sent to Kafka (requestId: $requestId)")
            
            val received = latch.await(REQUEST_TIMEOUT_SECONDS, TimeUnit.SECONDS)
            
            if (!received) {
                logger.error("Request timeout after $REQUEST_TIMEOUT_SECONDS seconds (requestId: $requestId)")
                throw RuntimeException("RPC request timeout for topic: $requestTopic")
            }
            
            val responseJson = pendingRequest.response as? String
                ?: throw RuntimeException("No response received for requestId: $requestId")
            
            val response = objectMapper.readValue(responseJson, responseType) 
            
            logger.info("Received RPC response for requestId: $requestId")
            return response
            
        } finally {
            pendingRequests.remove(requestId)
        }
    }
    
    fun getPendingProductById(id: Long): ProductResponse {
        logger.info("Requesting pending product with ID $id")
        
        val request = ProductServiceRequest(
            requestType = "GET_PENDING_PRODUCT_BY_ID",
            payload = GetProductPayload(id)
        )
        
        val response = sendRequest(
            requestTopic = PRODUCT_SERVICE_REQUEST_TOPIC,
            replyTopic = MODERATION_SERVICE_REPLY_TOPIC,
            request = request,
            responseType = object : TypeReference<ProductServiceResponse<ProductResponse>>() {}
        )
        
        if (!response.success || response.data == null) {
            throw ResourceNotFoundException("Product not found: $id")
        }
        
        return response.data
    }
    
    fun getPendingProducts(page: Int, pageSize: Int): PaginatedResponse<ProductResponse> {
        logger.info("Requesting pending products (page: $page, pageSize: $pageSize)")

        val request = ProductServiceRequest(
            requestType = "GET_PENDING_PRODUCTS",
            payload = GetProductsPayload(
                page = page,
                pageSize = pageSize
            )
        )
        
        @Suppress("UNCHECKED_CAST")
        val response = sendRequest(
            requestTopic = PRODUCT_SERVICE_REQUEST_TOPIC,
            replyTopic = MODERATION_SERVICE_REPLY_TOPIC,
            request = request,
            responseType = object : TypeReference<ProductServiceResponse<PaginatedResponse<ProductResponse>>>() {}
        )
        
        if (!response.success || response.data == null) {
            logger.warn("Product service returned error: ${response.errorMessage}")
            throw RuntimeException("Failed to fetch pending products: ${response.errorMessage}")
        }
        
        return response.data
    }
    
    fun approveProduct(id: Long, moderatorId: Long): ProductResponse {
        logger.info("Approving product $id by moderator $moderatorId")

        val request = ProductServiceRequest(
            requestType = "APPROVE_PRODUCT",
            payload = ApproveProductPayload(id, moderatorId)
        )
        
        @Suppress("UNCHECKED_CAST")
        val response = sendRequest(
            requestTopic = PRODUCT_SERVICE_REQUEST_TOPIC,
            replyTopic = MODERATION_SERVICE_REPLY_TOPIC,
            request = request,
            responseType = object : TypeReference<ProductServiceResponse<ProductResponse>>() {}
        )
        
        if (!response.success || response.data == null) {
            logger.warn("Product service returned error: ${response.errorMessage}")
            throw RuntimeException("Failed to approve product: ${response.errorMessage}")
        }
        
        return response.data
    }
    
    fun rejectProduct(id: Long, moderatorId: Long, reason: String): ProductResponse {
        logger.info("Rejecting product $id by moderator $moderatorId with reason: $reason")
        
        val request = ProductServiceRequest(
            requestType = "REJECT_PRODUCT",
            payload = RejectProductPayload(id, moderatorId, reason)
        )

        @Suppress("UNCHECKED_CAST")
        val response = sendRequest(
            requestTopic = PRODUCT_SERVICE_REQUEST_TOPIC,
            replyTopic = MODERATION_SERVICE_REPLY_TOPIC,
            request = request,
            responseType = object : TypeReference<ProductServiceResponse<ProductResponse>>() {}
        )
        
        if (!response.success || response.data == null) {
            logger.warn("Product service returned error: ${response.errorMessage}")
            throw RuntimeException("Failed to reject product: ${response.errorMessage}")
        }
        
        return response.data
    }
    
    fun getUserById(userId: Long): UserResponse {
        logger.info("Requesting user with ID $userId")
        
        val request = UserServiceRequest(
            requestType = "GET_USER_BY_ID",
            payload = GetUserPayload(userId)
        )

        val response = sendRequest(
            requestTopic = USER_SERVICE_REQUEST_TOPIC,
            replyTopic = MODERATION_SERVICE_REPLY_TOPIC,
            request = request,
            responseType = object : TypeReference<UserServiceResponse<UserResponse>>() {}
        )

        if (!response.success || response.data == null) {
            logger.warn("User service returned error: ${response.errorMessage}")
            throw RuntimeException("Failed to load user: ${response.errorMessage}")
        }
        
        return response.data
    }
    
    fun handleResponse(responseJson: String, requestId: String) {
        try {
            logger.debug("Handling response for requestId: $requestId")
            
            val pendingRequest = pendingRequests[requestId]
            if (pendingRequest == null) {
                logger.warn("Received response for unknown requestId: $requestId")
                return
            }
            
            pendingRequest.response = responseJson
            pendingRequest.latch.countDown()
            logger.debug("Response received for requestId: $requestId")
            
        } catch (e: Exception) {
            logger.error("Error processing response for requestId: $requestId", e)
            val pendingRequest = pendingRequests[requestId]
            pendingRequest?.latch?.countDown()
        }
    }
    
    private data class PendingRequest(
        val latch: CountDownLatch,
        var response: Any? = null
    )
}
