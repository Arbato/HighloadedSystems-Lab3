package ru.itmo.orderservice.presentation.controller

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import jakarta.validation.constraints.Min
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.*
import ru.itmo.orderservice.application.dto.request.CreateOrderRequest
import ru.itmo.orderservice.application.dto.response.OrderResponse
import ru.itmo.orderservice.application.dto.response.PaginatedResponse
import ru.itmo.orderservice.application.service.OrderService

@RestController
@RequestMapping("/api/orders")
@Tag(name = "Orders", description = "Order management API")
@Validated
class OrderController(
    private val orderService: OrderService
) {
    
    /**
     * POST /api/orders
     * Оформить заказ
     */
    @PostMapping
    @Operation(summary = "Create order from cart")
    @ApiResponses(
        ApiResponse(responseCode = "201", description = "Order created"),
        ApiResponse(responseCode = "400", description = "Invalid request or empty cart"),
        ApiResponse(responseCode = "404", description = "Cart not found"),
        ApiResponse(responseCode = "500", description = "Internal server error")
    )
    fun createOrder(
        @RequestHeader("X-User-Id")
        @Parameter(hidden = true)
        userId: Long,

        @Valid
        @RequestBody
        request: CreateOrderRequest
    ): ResponseEntity<OrderResponse> {
        val order = orderService.createOrder(userId, request)
        return ResponseEntity.status(HttpStatus.CREATED).body(order)
    }
    
    /**
     * GET /api/orders/{orderId}
     * Получить заказ по ID
     */
    @GetMapping("/{orderId}")
    @Operation(summary = "Get order by ID")
    @ApiResponses(
        ApiResponse(responseCode = "200", description = "Order found"),
        ApiResponse(responseCode = "400", description = "Invalid order ID"),
        ApiResponse(responseCode = "404", description = "Order not found"),
        ApiResponse(responseCode = "500", description = "Internal server error")
    )
    fun getOrder(
        @PathVariable
        @Min(1, message = "Order ID must be greater than 0")
        @Parameter(description = "Order ID")
        orderId: Long,

        @RequestHeader("X-User-Id")
        @Parameter(hidden = true)
        userId: Long
    ): ResponseEntity<OrderResponse> {
        val order = orderService.getOrderById(orderId, userId)
        return ResponseEntity.ok(order)
    }
    
    /**
     * GET /api/orders
     * Получить все заказы пользователя
     */
    @GetMapping
    @Operation(summary = "Get user's orders")
    @ApiResponses(
        ApiResponse(responseCode = "200", description = "Orders retrieved"),
        ApiResponse(responseCode = "400", description = "Invalid parameters"),
        ApiResponse(responseCode = "500", description = "Internal server error")
    )
    fun getUserOrders(
        @RequestHeader("X-User-Id")
        @Parameter(hidden = true)
        userId: Long,

        @RequestParam(defaultValue = "1")
        @Min(1, message = "Page must be greater than 0")
        @Parameter(description = "Page number")
        page: Int,

        @RequestParam(defaultValue = "20")
        @Min(1, message = "PageSize must be greater than 0")
        @Parameter(description = "Items per page")
        pageSize: Int
    ): ResponseEntity<PaginatedResponse<OrderResponse>> {
        val response = orderService.getUserOrders(userId, page, pageSize)
        return ResponseEntity.ok(response)
    }
}
