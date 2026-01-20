package ru.itmo.productservice.adapter.`in`.web

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import jakarta.validation.constraints.Min
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import ru.itmo.productservice.domain.model.dto.request.CreateShopRequest
import ru.itmo.productservice.domain.model.dto.request.UpdateShopRequest
import ru.itmo.productservice.domain.model.dto.response.PaginatedResponse
import ru.itmo.productservice.domain.model.dto.response.ProductResponse
import ru.itmo.productservice.domain.model.dto.response.ShopResponse
import ru.itmo.productservice.application.service.ShopService
import org.springframework.validation.annotation.Validated

@RestController
@RequestMapping("/api/shops")
@Validated
@Tag(name = "Shops", description = "Shop management API")
class ShopController(
    private val shopService: ShopService
) {
    
    /**
     * GET /api/shops
     * Получить все магазины (пагинация)
     */
    @GetMapping
    @Operation(summary = "Get all shops")
    @ApiResponses(
        ApiResponse(responseCode = "200", description = "Shops retrieved"),
        ApiResponse(responseCode = "400", description = "Invalid parameters"),
        ApiResponse(responseCode = "500", description = "Internal server error")
    )
    fun getAllShops(
        @RequestParam(defaultValue = "1")
        @Min(1, message = "Page must be greater than 0")
        @Parameter(description = "Page number", example = "1")
        page: Int,
        
        @RequestParam(defaultValue = "20")
        @Min(1, message = "PageSize must be greater than 0")
        @Parameter(description = "Items per page", example = "20")
        pageSize: Int
    ): ResponseEntity<PaginatedResponse<ShopResponse>> {
        val response = shopService.getAllShops(page, pageSize)
        return ResponseEntity.ok(response)
    }
    
    /**
     * GET /api/shops/{shopId}
     * Получить магазин по ID
     */
    @GetMapping("/{shopId}")
    @Operation(summary = "Get shop by ID")
    @ApiResponses(
        ApiResponse(responseCode = "200", description = "Shop found"),
        ApiResponse(responseCode = "400", description = "Invalid shop ID"),
        ApiResponse(responseCode = "404", description = "Shop not found"),
        ApiResponse(responseCode = "500", description = "Internal server error")
    )
    fun getShop(
        @PathVariable
        @Min(1, message = "Shop ID must be greater than 0")
        @Parameter(description = "Shop ID", example = "1")
        shopId: Long
    ): ResponseEntity<ShopResponse> {
        val response = shopService.getShopById(shopId)
        return ResponseEntity.ok(response)
    }
    
    /**
     * POST /api/shops
     * Создать магазин (только для продавца)
     */
    @PostMapping
    @Operation(summary = "Create shop")
    @ApiResponses(
        ApiResponse(responseCode = "201", description = "Shop created"),
        ApiResponse(responseCode = "400", description = "Invalid input"),
        ApiResponse(responseCode = "403", description = "User is not a seller"),
        ApiResponse(responseCode = "404", description = "User not found"),
        ApiResponse(responseCode = "500", description = "Internal server error")
    )
    fun createShop(
        @RequestHeader("X-User-Id")
        @Parameter(hidden = true)
        sellerId: Long,

        @Valid
        @RequestBody
        request: CreateShopRequest
    ): ResponseEntity<ShopResponse> {
        val response = shopService.createShop(sellerId, request)
        return ResponseEntity.status(HttpStatus.CREATED).body(response)
    }
    
    /**
     * PUT /api/shops/{shopId}
     * Обновить магазин (только владелец)
     */
    @PutMapping("/{shopId}")
    @Operation(summary = "Update shop")
    @ApiResponses(
        ApiResponse(responseCode = "200", description = "Shop updated"),
        ApiResponse(responseCode = "400", description = "Invalid input"),
        ApiResponse(responseCode = "403", description = "Not authorized"),
        ApiResponse(responseCode = "404", description = "Shop not found"),
        ApiResponse(responseCode = "500", description = "Internal server error")
    )
    fun updateShop(
        @PathVariable
        @Min(1, message = "Shop ID must be greater than 0")
        @Parameter(description = "Shop ID")
        shopId: Long,

        @RequestHeader("X-User-Id")
        @Parameter(hidden = true)
        sellerId: Long,

        @Valid
        @RequestBody
        request: UpdateShopRequest
    ): ResponseEntity<ShopResponse> {
        val response = shopService.updateShop(shopId, sellerId, request)
        return ResponseEntity.ok(response)
    }
    
    /**
     * GET /api/shops/{shopId}/products
     * Получить товары магазина (пагинация)
     */
    @GetMapping("/{shopId}/products")
    @Operation(summary = "Get shop products")
    @ApiResponses(
        ApiResponse(responseCode = "200", description = "Products retrieved"),
        ApiResponse(responseCode = "400", description = "Invalid parameters"),
        ApiResponse(responseCode = "404", description = "Shop not found"),
        ApiResponse(responseCode = "500", description = "Internal server error")
    )
    fun getShopProducts(
        @PathVariable
        @Min(1, message = "Shop ID must be greater than 0")
        @Parameter(description = "Shop ID")
        shopId: Long,
        
        @RequestParam(defaultValue = "1")
        @Min(1, message = "Page must be greater than 0")
        @Parameter(description = "Page number")
        page: Int,
        
        @RequestParam(defaultValue = "20")
        @Min(1, message = "PageSize must be greater than 0")
        @Parameter(description = "Items per page")
        pageSize: Int
    ): ResponseEntity<PaginatedResponse<ProductResponse>> {
        val response = shopService.getShopProducts(shopId, page, pageSize)
        return ResponseEntity.ok(response)
    }
}
