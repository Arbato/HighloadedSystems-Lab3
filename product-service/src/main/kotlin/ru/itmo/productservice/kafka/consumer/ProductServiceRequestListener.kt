package ru.itmo.productservice.kafka.consumer

import com.fasterxml.jackson.databind.ObjectMapper
import org.slf4j.LoggerFactory
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.kafka.support.KafkaHeaders
import org.springframework.messaging.handler.annotation.Header
import org.springframework.messaging.handler.annotation.Payload
import org.springframework.messaging.support.MessageBuilder
import org.springframework.stereotype.Component
import ru.itmo.productservice.model.dto.kafka.GetProductPayload
import ru.itmo.productservice.model.dto.kafka.ProductServiceResponse
import ru.itmo.productservice.model.dto.kafka.GetProductsPayload
import ru.itmo.productservice.model.dto.kafka.ApproveProductPayload
import ru.itmo.productservice.model.dto.kafka.RejectProductPayload
import ru.itmo.productservice.model.dto.response.ProductResponse
import ru.itmo.productservice.model.dto.response.PaginatedResponse
import ru.itmo.productservice.service.ProductService

@Component
class ProductServiceRequestListener(
    private val productService: ProductService,
    private val kafkaTemplate: KafkaTemplate<String, String>,
    private val objectMapper: ObjectMapper
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    @KafkaListener(
        topics = ["product-service-requests"],
        groupId = "product-service-request-processor"
    )
    fun handleProductRequest(
            @Payload requestJson: String,
            @Header("requestId") requestId: String,
            @Header("replyTopic", required = true) replyTopic: String
        ) {
            try {
                logger.info("Received RPC request: requestId=$requestId, replyTopic=$replyTopic")
                
                val requestNode = objectMapper.readTree(requestJson)
                val requestType = requestNode.get("request_type")?.asText()
                
                val response: ProductServiceResponse<*> = when (requestType) {
                    "GET_PENDING_PRODUCT_BY_ID" -> {
                        val payload = objectMapper.treeToValue(
                            requestNode.get("payload"),
                            GetProductPayload::class.java
                        )
                        handleGetPendingProductById(payload.productId)
                    }

                    "GET_PRODUCT_BY_ID" -> {
                        val payload = objectMapper.treeToValue(
                            requestNode.get("payload"),
                            GetProductPayload::class.java
                        )
                        handleGetProductById(payload.productId)
                    }
                    
                    "GET_PENDING_PRODUCTS" -> {
                        val payload = objectMapper.treeToValue(
                            requestNode.get("payload"),
                            GetProductsPayload::class.java
                        )
                        handleGetPendingProducts(payload.page, payload.pageSize)
                    }
                    
                    "APPROVE_PRODUCT" -> {
                        val payload = objectMapper.treeToValue(
                            requestNode.get("payload"),
                            ApproveProductPayload::class.java
                        )
                        handleApproveProduct(payload.productId, payload.moderatorId)
                    }
                    
                    "REJECT_PRODUCT" -> {
                        val payload = objectMapper.treeToValue(
                            requestNode.get("payload"),
                            RejectProductPayload::class.java
                        )
                        handleRejectProduct(payload.productId, payload.moderatorId, payload.reason)
                    }
                    
                    else -> {
                        logger.warn("Unknown request type: $requestType")
                        ProductServiceResponse<ProductResponse>(
                            success = false,
                            errorMessage = "Unknown request type: $requestType"
                        )
                    }
                }
                
                sendResponse(response, requestId, replyTopic)
                
            } catch (e: Exception) {
                logger.error("Error processing request: requestId=$requestId", e)
                val errorResponse = ProductServiceResponse<ProductResponse>(
                    success = false,
                    errorMessage = e.message ?: "Internal error"
                )
                sendResponse(errorResponse, requestId, replyTopic)
            }
        }

    private fun handleGetPendingProducts(page: Int, pageSize: Int): ProductServiceResponse<PaginatedResponse<ProductResponse>> {
        return try {
            val products = productService.getPendingProducts(page, pageSize)
            
            val productResponses = products.data.map { product ->
                ProductResponse(
                    id = product.id,
                    name = product.name,
                    description = product.description,
                    price = product.price,
                    imageUrl = product.imageUrl,
                    shopId = product.shopId,
                    sellerId = product.sellerId,
                    status = product.status,
                    rejectionReason = product.rejectionReason,
                    averageRating = product.averageRating,
                    commentsCount = product.commentsCount,
                    createdAt = product.createdAt,
                    updatedAt = product.updatedAt
                )
            }
            
            ProductServiceResponse(
                success = true,
                data = PaginatedResponse(
                    data = productResponses,
                    page = products.page,
                    pageSize = products.pageSize,
                    totalElements = products.totalElements,
                    totalPages = products.totalPages
                )
            )
        } catch (e: Exception) {
            logger.error("Error getting pending products (page: $page, pageSize: $pageSize)", e)
            ProductServiceResponse(
                success = false,
                errorMessage = e.message ?: "Internal error"
            )
        }
    }

    private fun handleGetPendingProductById(productId: Long): ProductServiceResponse<ProductResponse> {
        return try {
            val product = productService.getPendingProductById(productId)
            
            ProductServiceResponse(
                success = true,
                data = ProductResponse(
                    id = product.id,
                    name = product.name,
                    description = product.description,
                    price = product.price,
                    imageUrl = product.imageUrl,
                    shopId = product.shopId,
                    sellerId = product.sellerId,
                    status = product.status,
                    rejectionReason = product.rejectionReason,
                    averageRating = product.averageRating,
                    commentsCount = product.commentsCount,
                    createdAt = product.createdAt,
                    updatedAt = product.updatedAt
                )
            )
        } catch (e: Exception) {
            logger.error("Error getting product with id: $productId", e)
            ProductServiceResponse(
                success = false,
                errorMessage = e.message ?: "Internal error"
            )
        }
    }

    private fun handleGetProductById(productId: Long): ProductServiceResponse<ProductResponse> {
        return try {
            val product = productService.getProductById(productId)
            
            ProductServiceResponse(
                success = true,
                data = ProductResponse(
                    id = product.id,
                    name = product.name,
                    description = product.description,
                    price = product.price,
                    imageUrl = product.imageUrl,
                    shopId = product.shopId,
                    sellerId = product.sellerId,
                    status = product.status,
                    rejectionReason = product.rejectionReason,
                    averageRating = product.averageRating,
                    commentsCount = product.commentsCount,
                    createdAt = product.createdAt,
                    updatedAt = product.updatedAt
                )
            )
        } catch (e: Exception) {
            logger.error("Error getting product with id: $productId", e)
            ProductServiceResponse(
                success = false,
                errorMessage = e.message ?: "Internal error"
            )
        }
    }

    private fun handleRejectProduct(
        productId: Long,
        moderatorId: Long,
        reason: String
    ): ProductServiceResponse<ProductResponse> {
        return try {
            val product = productService.rejectProduct(productId, moderatorId, reason)
            
            ProductServiceResponse(
                success = true,
                data = ProductResponse(
                    id = product.id,
                    name = product.name,
                    description = product.description,
                    price = product.price,
                    imageUrl = product.imageUrl,
                    shopId = product.shopId,
                    sellerId = product.sellerId,
                    status = product.status,
                    rejectionReason = product.rejectionReason,
                    averageRating = product.averageRating,
                    commentsCount = product.commentsCount,
                    createdAt = product.createdAt,
                    updatedAt = product.updatedAt
                )
            )
        } catch (e: Exception) {
            logger.error("Error rejecting product with id: $productId by moderator: $moderatorId, reason: $reason", e)
            ProductServiceResponse(
                success = false,
                errorMessage = e.message ?: "Internal error"
            )
        }
    }

    private fun handleApproveProduct(productId: Long, moderatorId: Long): ProductServiceResponse<ProductResponse> {
        return try {
            val product = productService.approveProduct(productId, moderatorId)
            
            ProductServiceResponse(
                success = true,
                data = ProductResponse(
                    id = product.id,
                    name = product.name,
                    description = product.description,
                    price = product.price,
                    imageUrl = product.imageUrl,
                    shopId = product.shopId,
                    sellerId = product.sellerId,
                    status = product.status,
                    rejectionReason = product.rejectionReason,
                    averageRating = product.averageRating,
                    commentsCount = product.commentsCount,
                    createdAt = product.createdAt,
                    updatedAt = product.updatedAt
                )
            )
        } catch (e: Exception) {
            logger.error("Error approving product with id: $productId by moderator: $moderatorId", e)
            ProductServiceResponse(
                success = false,
                errorMessage = e.message ?: "Internal error"
            )
        }
    }

    private fun sendResponse(
        response: ProductServiceResponse<*>,
        requestId: String,
        replyTopic: String
    ) {
        try {
            val responseJson = objectMapper.writeValueAsString(response)
            val message = MessageBuilder.withPayload(responseJson)
                .setHeader(KafkaHeaders.TOPIC, replyTopic)
                .setHeader("requestId", requestId)
                .build()
            
            kafkaTemplate.send(message)
            logger.info("Sent RPC response: requestId=$requestId, replyTopic=$replyTopic, success=${response.success}")
        } catch (e: Exception) {
            logger.error("Error sending response: requestId=$requestId", e)
        }
    }
}
