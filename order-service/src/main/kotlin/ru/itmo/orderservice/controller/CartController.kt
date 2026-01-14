package ru.itmo.orderservice.controller

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import jakarta.validation.constraints.Min
import org.springframework.http.HttpStatus
import org.springframework.validation.annotation.Validated
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import ru.itmo.orderservice.model.dto.request.AddToCartRequest
import ru.itmo.orderservice.model.dto.request.UpdateQuantityRequest
import ru.itmo.orderservice.model.dto.response.OrderResponse
import ru.itmo.orderservice.service.OrderService

@RestController
@RequestMapping("/api/cart")
@Tag(name = "Cart", description = "Shopping cart management API")
@Validated
class CartController(
    private val orderService: OrderService
) {
    
    /**
     * GET /api/cart
     * Получить корзину пользователя
     */
    @GetMapping
    @Operation(summary = "Get user's shopping cart")
    @ApiResponses(
        ApiResponse(responseCode = "200", description = "Cart retrieved"),
        ApiResponse(responseCode = "400", description = "Invalid user ID"),
        ApiResponse(responseCode = "500", description = "Internal server error")
    )
    fun getCart(
        @RequestHeader("X-User-Id")
        @Parameter(hidden = true)
        userId: Long
    ): ResponseEntity<OrderResponse> {
        val cart = orderService.getCart(userId)
        return ResponseEntity.ok(cart)
    }
    
    /**
     * POST /api/cart/items
     * Добавить товар в корзину
     */
    @PostMapping("/items")
    @Operation(summary = "Add item to cart")
    @ApiResponses(
        ApiResponse(responseCode = "200", description = "Item added"),
        ApiResponse(responseCode = "400", description = "Invalid request"),
        ApiResponse(responseCode = "404", description = "Product not found"),
        ApiResponse(responseCode = "500", description = "Internal server error")
    )
    fun addToCart(
        @RequestHeader("X-User-Id")
        @Parameter(hidden = true)
        userId: Long,

        @Valid
        @RequestBody
        request: AddToCartRequest
    ): ResponseEntity<OrderResponse> {
        val cart = orderService.addToCart(userId, request.productId, request.quantity)
        return ResponseEntity.ok(cart)
    }
    
    /**
     * PUT /api/cart/items/{itemId}
     * Обновить количество товара
     */
    @PutMapping("/items/{itemId}")
    @Operation(summary = "Update cart item quantity")
    @ApiResponses(
        ApiResponse(responseCode = "200", description = "Quantity updated"),
        ApiResponse(responseCode = "400", description = "Invalid request"),
        ApiResponse(responseCode = "404", description = "Item not found"),
        ApiResponse(responseCode = "500", description = "Internal server error")
    )
    fun updateCartItem(
        @PathVariable
        @Min(1, message = "Item ID must be greater than 0")
        @Parameter(description = "Item ID")
        itemId: Long,

        @RequestHeader("X-User-Id")
        @Parameter(hidden = true)
        userId: Long,

        @Valid
        @RequestBody
        request: UpdateQuantityRequest
    ): ResponseEntity<OrderResponse> {
        val cart = orderService.updateCartItemQuantity(userId, itemId, request.quantity)
        return ResponseEntity.ok(cart)
    }
    
    /**
     * DELETE /api/cart/items/{itemId}
     * Удалить товар из корзины
     */
    @DeleteMapping("/items/{itemId}")
    @Operation(summary = "Remove item from cart")
    @ApiResponses(
        ApiResponse(responseCode = "200", description = "Item removed"),
        ApiResponse(responseCode = "400", description = "Invalid request"),
        ApiResponse(responseCode = "404", description = "Item not found"),
        ApiResponse(responseCode = "500", description = "Internal server error")
    )
    fun removeFromCart(
        @RequestHeader("X-User-Id")
        @Parameter(hidden = true)
        userId: Long,

        @PathVariable
        @Min(1, message = "Item ID must be greater than 0")
        @Parameter(description = "Item ID")
        itemId: Long
    ): ResponseEntity<OrderResponse> {
        val cart = orderService.removeFromCart(userId, itemId)
        return ResponseEntity.ok(cart)
    }

    /**
     * DELETE /api/cart
     * Очистить корзину
     */
    @DeleteMapping
    @Operation(summary = "Clear shopping cart")
    @ApiResponses(
        ApiResponse(responseCode = "204", description = "Cart cleared"),
        ApiResponse(responseCode = "400", description = "Invalid user ID"),
        ApiResponse(responseCode = "404", description = "Cart not found"),
        ApiResponse(responseCode = "500", description = "Internal server error")
    )
    fun clearCart(
        @RequestHeader("X-User-Id")
        @Parameter(hidden = true)
        userId: Long
    ): ResponseEntity<Void> {
        orderService.clearCart(userId)
        return ResponseEntity.noContent().build()
    }
}
