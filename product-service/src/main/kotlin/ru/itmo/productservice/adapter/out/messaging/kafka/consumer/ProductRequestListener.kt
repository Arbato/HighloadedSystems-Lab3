package ru.itmo.productservice.adapter.out.messaging.kafka.consumer

import com.fasterxml.jackson.databind.ObjectMapper
import org.slf4j.LoggerFactory
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.kafka.support.KafkaHeaders
import org.springframework.messaging.handler.annotation.Header
import org.springframework.messaging.handler.annotation.Payload
import org.springframework.messaging.support.MessageBuilder
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.stereotype.Component
import ru.itmo.productservice.application.service.ProductService
import ru.itmo.productservice.domain.model.dto.kafka.ProductServiceRequest
import ru.itmo.productservice.domain.model.dto.kafka.ProductServiceResponse
import ru.itmo.productservice.domain.model.dto.kafka.ProductData

@Component
@ConditionalOnProperty(name = ["spring.kafka.enabled"], havingValue = "true", matchIfMissing = true)
class ProductRequestListener(
    private val productService: ProductService,
    private val kafkaTemplate: KafkaTemplate<String, String>,
    private val objectMapper: ObjectMapper
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    @KafkaListener(
        topics = ["product-service-requests"],
        groupId = "product-service-request-processor"
    )
    fun handleProductServiceRequest(
        @Payload requestJson: String,
        @Header("requestId") requestId: String,
        @Header("replyTopic", required = false) replyTopic: String? = "order-service-replies"
    ) {
        try {
            logger.info("Received RPC request: requestId=$requestId")

            val request = objectMapper.readValue(requestJson, ProductServiceRequest::class.java)

            val response = when (request.requestType) {
                "GET_PRODUCT_BY_ID" -> handleGetProductById(request.productId!!)
                "GET_PENDING_PRODUCT_BY_ID" -> handleGetPendingProductById(request.productId!!)
                "GET_PENDING_PRODUCTS" -> handleGetPendingProducts(request.page ?: 1, request.pageSize ?: 20)
                "APPROVE_PRODUCT" -> handleApproveProduct(request.productId!!, request.moderatorId!!)
                "REJECT_PRODUCT" -> handleRejectProduct(request.productId!!, request.moderatorId!!, request.reason ?: "")
                else -> {
                    logger.warn("Unknown request type: ${request.requestType}")
                    ProductServiceResponse(
                        requestId = requestId,
                        success = false,
                        errorMessage = "Unknown request type: ${request.requestType}"
                    )
                }
            }

            sendResponse(response.copy(requestId = requestId), requestId, replyTopic!!)

        } catch (e: Exception) {
            logger.error("Error processing request: requestId=$requestId", e)
            val errorResponse = ProductServiceResponse(
                requestId = requestId,
                success = false,
                errorMessage = e.message ?: "Internal error"
            )
            sendResponse(errorResponse, requestId, replyTopic ?: "order-service-replies")
        }
    }

    private fun handleGetProductById(productId: Long): ProductServiceResponse {
        return try {
            val product = productService.getProductById(productId)

            ProductServiceResponse(
                requestId = "",
                success = true,
                product = ProductData(
                    id = product.id,
                    name = product.name,
                    description = product.description,
                    price = product.price,
                    shopId = product.shopId,
                    status = product.status
                )
            )
        } catch (e: Exception) {
            ProductServiceResponse(
                requestId = "",
                success = false,
                errorMessage = e.message ?: "Product not found"
            )
        }
    }

    private fun handleGetPendingProductById(productId: Long): ProductServiceResponse {
        return try {
            val product = productService.getPendingProductById(productId)

            ProductServiceResponse(
                requestId = "",
                success = true,
                product = ProductData(
                    id = product.id,
                    name = product.name,
                    description = product.description,
                    price = product.price,
                    shopId = product.shopId,
                    status = product.status
                )
            )
        } catch (e: Exception) {
            ProductServiceResponse(
                requestId = "",
                success = false,
                errorMessage = e.message ?: "Pending product not found"
            )
        }
    }

    private fun handleGetPendingProducts(page: Int, pageSize: Int): ProductServiceResponse {
        return try {
            val products = productService.getPendingProducts(page, pageSize)

            ProductServiceResponse(
                requestId = "",
                success = true,
                products = products.data.map { product ->
                    ProductData(
                        id = product.id,
                        name = product.name,
                        description = product.description,
                        price = product.price,
                        shopId = product.shopId,
                        status = product.status
                    )
                },
                totalElements = products.totalElements,
                totalPages = products.totalPages,
                page = products.page,
                pageSize = products.pageSize
            )
        } catch (e: Exception) {
            ProductServiceResponse(
                requestId = "",
                success = false,
                errorMessage = e.message ?: "Error fetching pending products"
            )
        }
    }

    private fun handleApproveProduct(productId: Long, moderatorId: Long): ProductServiceResponse {
        return try {
            val product = productService.approveProduct(productId, moderatorId)

            ProductServiceResponse(
                requestId = "",
                success = true,
                product = ProductData(
                    id = product.id,
                    name = product.name,
                    description = product.description,
                    price = product.price,
                    shopId = product.shopId,
                    status = product.status
                )
            )
        } catch (e: Exception) {
            ProductServiceResponse(
                requestId = "",
                success = false,
                errorMessage = e.message ?: "Error approving product"
            )
        }
    }

    private fun handleRejectProduct(productId: Long, moderatorId: Long, reason: String): ProductServiceResponse {
        return try {
            val product = productService.rejectProduct(productId, moderatorId, reason)

            ProductServiceResponse(
                requestId = "",
                success = true,
                product = ProductData(
                    id = product.id,
                    name = product.name,
                    description = product.description,
                    price = product.price,
                    shopId = product.shopId,
                    status = product.status
                )
            )
        } catch (e: Exception) {
            ProductServiceResponse(
                requestId = "",
                success = false,
                errorMessage = e.message ?: "Error rejecting product"
            )
        }
    }

    private fun sendResponse(
        response: ProductServiceResponse,
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
            logger.info("Sent RPC response: requestId=$requestId, success=${response.success}")
        } catch (e: Exception) {
            logger.error("Error sending response: requestId=$requestId", e)
        }
    }
}
