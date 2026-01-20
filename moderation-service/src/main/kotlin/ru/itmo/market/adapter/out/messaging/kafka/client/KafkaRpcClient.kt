package ru.itmo.market.adapter.out.messaging.kafka.client

import com.fasterxml.jackson.databind.ObjectMapper
import org.slf4j.LoggerFactory
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.kafka.support.KafkaHeaders
import org.springframework.messaging.Message
import org.springframework.messaging.support.MessageBuilder
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.stereotype.Component
import ru.itmo.market.domain.model.dto.kafka.*
import ru.itmo.market.domain.model.dto.response.ProductResponse
import ru.itmo.market.domain.model.dto.response.PaginatedResponse
import ru.itmo.market.domain.model.dto.response.UserResponse
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
    private val pendingProductRequests: ConcurrentHashMap<String, PendingProductRequest> = ConcurrentHashMap()
    private val pendingUserRequests: ConcurrentHashMap<String, PendingUserRequest> = ConcurrentHashMap()

    companion object {
        private const val PRODUCT_SERVICE_REQUEST_TOPIC = "product-service-requests"
        private const val USER_SERVICE_REQUEST_TOPIC = "user-service-requests"
        private const val MODERATION_SERVICE_REPLY_TOPIC = "moderation-service-replies"
        private const val REQUEST_TIMEOUT_SECONDS = 10L
    }

    // ========== PRODUCT SERVICE METHODS ==========

    fun getPendingProducts(page: Int, pageSize: Int): PaginatedResponse<ProductResponse> {
        val requestId = UUID.randomUUID().toString()
        logger.info("Requesting pending products (page=$page, pageSize=$pageSize, requestId=$requestId)")

        val request = ProductServiceRequest(
            requestId = requestId,
            requestType = "GET_PENDING_PRODUCTS",
            page = page,
            pageSize = pageSize
        )

        val response = sendProductRequest(requestId, request)

        val products = response.products?.map { productData ->
            ProductResponse(
                id = productData.id,
                name = productData.name,
                description = productData.description,
                price = productData.price,
                imageUrl = productData.imageUrl,
                shopId = productData.shopId,
                sellerId = productData.sellerId,
                status = productData.status,
                rejectionReason = productData.rejectionReason,
                createdAt = java.time.LocalDateTime.now(),
                updatedAt = java.time.LocalDateTime.now()
            )
        } ?: emptyList()

        return PaginatedResponse(
            data = products,
            page = response.page ?: page,
            pageSize = response.pageSize ?: pageSize,
            totalElements = response.totalElements ?: 0L,
            totalPages = response.totalPages ?: 0
        )
    }

    fun getPendingProductById(productId: Long): ProductResponse {
        val requestId = UUID.randomUUID().toString()
        logger.info("Requesting pending product $productId (requestId=$requestId)")

        val request = ProductServiceRequest(
            requestId = requestId,
            requestType = "GET_PENDING_PRODUCT_BY_ID",
            productId = productId
        )

        val response = sendProductRequest(requestId, request)
        val product = response.product ?: throw RuntimeException("Product not found: $productId")

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

    fun approveProduct(productId: Long, moderatorId: Long): ProductResponse {
        val requestId = UUID.randomUUID().toString()
        logger.info("Approving product $productId by moderator $moderatorId (requestId=$requestId)")

        val request = ProductServiceRequest(
            requestId = requestId,
            requestType = "APPROVE_PRODUCT",
            productId = productId,
            moderatorId = moderatorId
        )

        val response = sendProductRequest(requestId, request)
        val product = response.product ?: throw RuntimeException("Failed to approve product: $productId")

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

    fun rejectProduct(productId: Long, moderatorId: Long, reason: String): ProductResponse {
        val requestId = UUID.randomUUID().toString()
        logger.info("Rejecting product $productId by moderator $moderatorId (requestId=$requestId)")

        val request = ProductServiceRequest(
            requestId = requestId,
            requestType = "REJECT_PRODUCT",
            productId = productId,
            moderatorId = moderatorId,
            reason = reason
        )

        val response = sendProductRequest(requestId, request)
        val product = response.product ?: throw RuntimeException("Failed to reject product: $productId")

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

    // ========== USER SERVICE METHODS ==========

    fun getUserById(userId: Long): UserResponse {
        val requestId = UUID.randomUUID().toString()
        logger.info("Requesting user $userId (requestId=$requestId)")

        val request = UserServiceRequest(
            requestId = requestId,
            requestType = "GET_USER_BY_ID",
            userId = userId
        )

        val response = sendUserRequest(requestId, request)
        val user = response.user ?: throw RuntimeException("User not found: $userId")

        return UserResponse(
            id = user.id,
            username = user.username,
            email = user.email,
            firstName = user.firstName,
            lastName = user.lastName,
            roles = user.roles,
            createdAt = user.createdAt,
            updatedAt = user.updatedAt
        )
    }

    // ========== INTERNAL METHODS ==========

    private fun sendProductRequest(requestId: String, request: ProductServiceRequest): ProductServiceResponse {
        val latch = CountDownLatch(1)
        val pendingRequest = PendingProductRequest(latch = latch)
        pendingProductRequests[requestId] = pendingRequest

        try {
            val requestJson = objectMapper.writeValueAsString(request)

            val message: Message<String> = MessageBuilder
                .withPayload(requestJson)
                .setHeader(KafkaHeaders.TOPIC, PRODUCT_SERVICE_REQUEST_TOPIC)
                .setHeader("requestId", requestId)
                .setHeader("replyTopic", MODERATION_SERVICE_REPLY_TOPIC)
                .build()

            kafkaTemplate.send(message)
            logger.debug("Product request sent to Kafka (requestId: $requestId)")

            val received = latch.await(REQUEST_TIMEOUT_SECONDS, TimeUnit.SECONDS)

            if (!received) {
                logger.error("Product request timeout (requestId: $requestId)")
                throw RuntimeException("Product service request timeout")
            }

            val response = pendingRequest.response
                ?: throw RuntimeException("No response received from product service")

            if (!response.success) {
                throw RuntimeException("Product service error: ${response.errorMessage}")
            }

            return response

        } finally {
            pendingProductRequests.remove(requestId)
        }
    }

    private fun sendUserRequest(requestId: String, request: UserServiceRequest): UserServiceResponse {
        val latch = CountDownLatch(1)
        val pendingRequest = PendingUserRequest(latch = latch)
        pendingUserRequests[requestId] = pendingRequest

        try {
            val requestJson = objectMapper.writeValueAsString(request)

            val message: Message<String> = MessageBuilder
                .withPayload(requestJson)
                .setHeader(KafkaHeaders.TOPIC, USER_SERVICE_REQUEST_TOPIC)
                .setHeader("requestId", requestId)
                .setHeader("replyTopic", MODERATION_SERVICE_REPLY_TOPIC)
                .build()

            kafkaTemplate.send(message)
            logger.debug("User request sent to Kafka (requestId: $requestId)")

            val received = latch.await(REQUEST_TIMEOUT_SECONDS, TimeUnit.SECONDS)

            if (!received) {
                logger.error("User request timeout (requestId: $requestId)")
                throw RuntimeException("User service request timeout")
            }

            val response = pendingRequest.response
                ?: throw RuntimeException("No response received from user service")

            if (!response.success) {
                throw RuntimeException("User service error: ${response.errorMessage}")
            }

            return response

        } finally {
            pendingUserRequests.remove(requestId)
        }
    }

    fun handleProductResponse(responseJson: String, requestId: String) {
        try {
            logger.debug("Handling product response for requestId: $requestId")
            val response = objectMapper.readValue(responseJson, ProductServiceResponse::class.java)

            val pendingRequest = pendingProductRequests[requestId]
            if (pendingRequest != null) {
                pendingRequest.response = response
                pendingRequest.latch.countDown()
            } else {
                logger.warn("Received product response for unknown requestId: $requestId")
            }
        } catch (e: Exception) {
            logger.error("Error processing product response for requestId: $requestId", e)
            pendingProductRequests[requestId]?.latch?.countDown()
        }
    }

    fun handleUserResponse(responseJson: String, requestId: String) {
        try {
            logger.debug("Handling user response for requestId: $requestId")
            val response = objectMapper.readValue(responseJson, UserServiceResponse::class.java)

            val pendingRequest = pendingUserRequests[requestId]
            if (pendingRequest != null) {
                pendingRequest.response = response
                pendingRequest.latch.countDown()
            } else {
                logger.warn("Received user response for unknown requestId: $requestId")
            }
        } catch (e: Exception) {
            logger.error("Error processing user response for requestId: $requestId", e)
            pendingUserRequests[requestId]?.latch?.countDown()
        }
    }

    private data class PendingProductRequest(
        val latch: CountDownLatch,
        var response: ProductServiceResponse? = null
    )

    private data class PendingUserRequest(
        val latch: CountDownLatch,
        var response: UserServiceResponse? = null
    )
}
