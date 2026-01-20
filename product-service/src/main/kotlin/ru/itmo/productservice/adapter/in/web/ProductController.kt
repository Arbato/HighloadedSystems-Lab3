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
import ru.itmo.productservice.domain.model.dto.request.CreateProductRequest
import ru.itmo.productservice.domain.model.dto.request.UpdateProductRequest
import ru.itmo.productservice.domain.model.dto.response.PaginatedResponse
import ru.itmo.productservice.domain.model.dto.response.ProductResponse
import ru.itmo.productservice.application.service.ProductService
import org.springframework.validation.annotation.Validated

@RestController
@RequestMapping("/api/products")
@Tag(name = "Products", description = "Product catalog API")
@Validated
class ProductController(
    private val productService: ProductService
) {
    
    /**
     * GET /api/products
     * Получить все одобренные товары (пагинация)
     */
    @GetMapping
    @Operation(
        summary = "Get all approved products",
        description = "Returns paginated list of approved products"
    )
    @ApiResponses(
        ApiResponse(responseCode = "200", description = "Products retrieved"),
        ApiResponse(responseCode = "400", description = "Invalid pagination parameters"),
        ApiResponse(responseCode = "500", description = "Internal server error")
    )
    fun getAllProducts(
        @RequestParam(defaultValue = "1")
        @Min(1, message = "Page must be greater than 0")
        @Parameter(description = "Page number", example = "1")
        page: Int,
        
        @RequestParam(defaultValue = "20")
        @Min(1, message = "PageSize must be greater than 0")
        @Parameter(description = "Items per page", example = "20")
        pageSize: Int
    ): ResponseEntity<PaginatedResponse<ProductResponse>> {
        val response = productService.getApprovedProducts(page, pageSize)
        return ResponseEntity.ok(response)
    }
    
    /**
     * GET /api/products/{productId}
     * Получить товар по ID
     */
    @GetMapping("/{productId}")
    @Operation(summary = "Get product by ID")
    @ApiResponses(
        ApiResponse(responseCode = "200", description = "Product found"),
        ApiResponse(responseCode = "400", description = "Invalid product ID"),
        ApiResponse(responseCode = "404", description = "Product not found"),
        ApiResponse(responseCode = "500", description = "Internal server error")
    )
    fun getProduct(
        @PathVariable
        @Min(1, message = "Product ID must be greater than 0")
        @Parameter(description = "Product ID", example = "1")
        productId: Long
    ): ResponseEntity<ProductResponse> {
        val response = productService.getProductById(productId)
        return ResponseEntity.ok(response)
    }
    
    /**
     * GET /api/products/search
     * Поиск товаров по ключевым словам
     */
    @GetMapping("/search")
    @Operation(summary = "Search products by keywords")
    @ApiResponses(
        ApiResponse(responseCode = "200", description = "Search results"),
        ApiResponse(responseCode = "400", description = "Invalid parameters"),
        ApiResponse(responseCode = "500", description = "Internal server error")
    )
    fun searchProducts(
        @RequestParam("keywords")
        @Parameter(description = "Search keywords")
        keywords: String,
        
        @RequestParam(defaultValue = "1")
        @Min(1, message = "Page must be greater than 0")
        @Parameter(description = "Page number")
        page: Int,
        
        @RequestParam(defaultValue = "20")
        @Min(1, message = "PageSize must be greater than 0")
        @Parameter(description = "Items per page")
        pageSize: Int
    ): ResponseEntity<PaginatedResponse<ProductResponse>> {
        val response = productService.searchProducts(keywords, page, pageSize)
        return ResponseEntity.ok(response)
    }
    
    /**
     * POST /api/products
     * Создать новый товар (только для продавца)
     */
    @PostMapping
    @Operation(summary = "Create new product")
    @ApiResponses(
        ApiResponse(responseCode = "201", description = "Product created"),
        ApiResponse(responseCode = "400", description = "Invalid input"),
        ApiResponse(responseCode = "403", description = "Not authorized"),
        ApiResponse(responseCode = "404", description = "Shop not found"),
        ApiResponse(responseCode = "500", description = "Internal server error")
    )
    fun createProduct(
        @RequestHeader("X-User-Id")
        @Parameter(hidden = true)
        sellerId: Long,

        @Valid
        @RequestBody
        request: CreateProductRequest
    ): ResponseEntity<ProductResponse> {
        val response = productService.createProduct(request, sellerId)
        return ResponseEntity.status(HttpStatus.CREATED).body(response)
    }
    
    /**
     * PUT /api/products/{productId}
     * Обновить товар (только для модератора)
     */
    @PutMapping("/{productId}")
    @Operation(summary = "Update product")
    @ApiResponses(
        ApiResponse(responseCode = "200", description = "Product updated"),
        ApiResponse(responseCode = "400", description = "Invalid input"),
        ApiResponse(responseCode = "403", description = "Not authorized"),
        ApiResponse(responseCode = "404", description = "Product not found"),
        ApiResponse(responseCode = "500", description = "Internal server error")
    )
    fun updateProduct(
        @PathVariable
        @Min(1, message = "Product ID must be greater than 0")
        @Parameter(description = "Product ID")
        productId: Long,

        @RequestHeader("X-User-Id")
        @Parameter(hidden = true)
        userId: Long,

        @Valid
        @RequestBody
        request: UpdateProductRequest
    ): ResponseEntity<ProductResponse> {
        val response = productService.updateProduct(productId, userId, request)
        return ResponseEntity.ok(response)
    }
    
    /**
     * DELETE /api/products/{productId}
     * Удалить товар
     */
    @DeleteMapping("/{productId}")
    @Operation(summary = "Delete product")
    @ApiResponses(
        ApiResponse(responseCode = "204", description = "Product deleted"),
        ApiResponse(responseCode = "400", description = "Invalid product ID"),
        ApiResponse(responseCode = "403", description = "Not authorized"),
        ApiResponse(responseCode = "404", description = "Product not found"),
        ApiResponse(responseCode = "500", description = "Internal server error")
    )
    fun deleteProduct(
        @PathVariable
        @Min(1, message = "Product ID must be greater than 0")
        @Parameter(description = "Product ID")
        productId: Long,

        @RequestHeader("X-User-Id")
        @Parameter(hidden = true)
        userId: Long
    ): ResponseEntity<Void> {
        productService.deleteProduct(productId, userId)
        return ResponseEntity.noContent().build()
    }

    /**
     * GET /api/products/pending
     * Получить товары на модерации (пагинация)
     */
    @GetMapping("/pending")
    @Operation(
        summary = "Get pending products",
        description = "Returns paginated list of products awaiting moderation"
    )
    @ApiResponses(
        ApiResponse(responseCode = "200", description = "Pending products retrieved"),
        ApiResponse(responseCode = "400", description = "Invalid pagination parameters"),
        ApiResponse(responseCode = "500", description = "Internal server error")
    )
    fun getPendingProducts(
        @RequestParam(defaultValue = "1")
        @Min(1, message = "Page must be greater than 0")
        @Parameter(description = "Page number", example = "1")
        page: Int,
        
        @RequestParam(defaultValue = "20")
        @Min(1, message = "PageSize must be greater than 0")
        @Parameter(description = "Items per page", example = "20")
        pageSize: Int
    ): ResponseEntity<PaginatedResponse<ProductResponse>> {
        val response = productService.getPendingProducts(page, pageSize)
        return ResponseEntity.ok(response)
    }

    @GetMapping("/pending/{id}")
    @Operation(
        summary = "Get pending product by id",
        description = "Returns pending product"
    )
    @ApiResponses(
        ApiResponse(responseCode = "200", description = "Product retrieved"),
        ApiResponse(responseCode = "400", description = "Invalid product id"),
        ApiResponse(responseCode = "500", description = "Internal server error")
    )
    fun getPendingProductById(
        @PathVariable 
        id: Long
    ): ResponseEntity<ProductResponse> {
        val response = productService.getPendingProductById(id)
        return ResponseEntity.ok(response)
    }

    /**
     * POST /api/products/{id}/approve
     * Одобрить товар (только для модератора)
     */
    @PostMapping("/{id}/approve")
    @Operation(summary = "Approve product")
    @ApiResponses(
        ApiResponse(responseCode = "200", description = "Product approved"),
        ApiResponse(responseCode = "400", description = "Invalid product ID"),
        ApiResponse(responseCode = "403", description = "Not authorized"),
        ApiResponse(responseCode = "404", description = "Product not found"),
        ApiResponse(responseCode = "500", description = "Internal server error")
    )
    fun approveProduct(
        @PathVariable
        @Min(1, message = "Product ID must be greater than 0")
        @Parameter(description = "Product ID")
        id: Long,

        @RequestHeader("X-User-Id")
        @Parameter(hidden = true)
        moderatorId: Long
    ): ResponseEntity<ProductResponse> {
        val response = productService.approveProduct(id, moderatorId)
        return ResponseEntity.ok(response)
    }
    
    /**
     * POST /api/products/{id}/reject
     * Отклонить товар (только для модератора)
     */
    @PostMapping("/{id}/reject")
    @Operation(summary = "Reject product")
    @ApiResponses(
        ApiResponse(responseCode = "200", description = "Product rejected"),
        ApiResponse(responseCode = "400", description = "Invalid input"),
        ApiResponse(responseCode = "403", description = "Not authorized"),
        ApiResponse(responseCode = "404", description = "Product not found"),
        ApiResponse(responseCode = "500", description = "Internal server error")
    )
    fun rejectProduct(
        @PathVariable
        @Min(1, message = "Product ID must be greater than 0")
        @Parameter(description = "Product ID")
        id: Long,

        @RequestHeader("X-User-Id")
        @Parameter(hidden = true)
        moderatorId: Long,

        @RequestParam
        @Parameter(description = "Rejection reason")
        reason: String
    ): ResponseEntity<ProductResponse> {
        val response = productService.rejectProduct(id, moderatorId, reason)
        return ResponseEntity.ok(response)
    }

}
