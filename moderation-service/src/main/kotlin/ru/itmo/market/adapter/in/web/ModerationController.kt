package ru.itmo.market.adapter.`in`.web

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import jakarta.validation.constraints.Min
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import ru.itmo.market.domain.model.dto.request.BulkModerationRequest
import ru.itmo.market.domain.model.dto.request.RejectProductRequest
import ru.itmo.market.domain.model.dto.response.ModerationResultResponse
import ru.itmo.market.domain.model.dto.response.PaginatedResponse
import ru.itmo.market.domain.model.dto.response.ProductResponse
import ru.itmo.market.application.service.ModerationService
import org.springframework.validation.annotation.Validated

@RestController
@RequestMapping("/api/moderation")
@Tag(name = "Moderation", description = "Product moderation API")
@Validated
class ModerationController(
    private val moderationService: ModerationService
) {
    
    // ========== GET МЕТОДЫ ==========
    
    @GetMapping("/products")
    @Operation(
        summary = "Get pending products",
        description = "Retrieve a list of products with PENDING status for the moderator"
    )
    @ApiResponses(value = [
        ApiResponse(responseCode = "200", description = "Products retrieved successfully"),
        ApiResponse(responseCode = "403", description = "User is not a moderator"),
        ApiResponse(responseCode = "500", description = "Internal server error")
    ])
    fun getPendingProducts(
        @RequestHeader("X-User-Id")
        @Parameter(hidden = true)
        moderatorId: Long,

        @RequestParam(defaultValue = "1")
        @Parameter(description = "Page number", example = "1")
        page: Int,

        @RequestParam(defaultValue = "20")
        @Parameter(description = "Page size", example = "20")
        pageSize: Int
    ): Mono<ResponseEntity<PaginatedResponse<ProductResponse>>> {
        return moderationService.getPendingProducts(moderatorId, page, pageSize)
            .map { ResponseEntity.ok(it) }
    }
    
    @GetMapping("/products/{id}")
    @Operation(
        summary = "Get pending product by ID",
        description = "Retrieve detailed information about a product pending moderation"
    )
    @ApiResponses(value = [
        ApiResponse(responseCode = "200", description = "Product found"),
        ApiResponse(responseCode = "403", description = "User is not a moderator"),
        ApiResponse(responseCode = "404", description = "Product not found"),
        ApiResponse(responseCode = "500", description = "Internal server error")
    ])
    fun getPendingProductById(
        @RequestHeader("X-User-Id")
        @Parameter(hidden = true)
        moderatorId: Long,

        @PathVariable
        @Parameter(description = "Product ID", example = "100")
        id: Long
    ): Mono<ResponseEntity<ProductResponse>> {
        return moderationService.getPendingProductById(moderatorId, id)
            .map { ResponseEntity.ok(it) }
    }
    
    // ========== POST МЕТОДЫ ==========
    
    @PostMapping("/products/{id}/approve")
    @Operation(
        summary = "Approve product",
        description = "Approve a product and change its status to APPROVED"
    )
    @ApiResponses(value = [
        ApiResponse(responseCode = "200", description = "Product approved successfully"),
        ApiResponse(responseCode = "403", description = "User is not a moderator"),
        ApiResponse(responseCode = "404", description = "Product not found"),
        ApiResponse(responseCode = "500", description = "Internal server error")
    ])
    fun approveProduct(
        @RequestHeader("X-User-Id")
        @Parameter(hidden = true)
        moderatorId: Long,

        @PathVariable
        @Parameter(description = "Product ID", example = "100")
        id: Long
    ): Mono<ResponseEntity<ModerationResultResponse>> {
        return moderationService.approveProduct(moderatorId, id)
            .map { ResponseEntity.status(HttpStatus.OK).body(it) }
    }
    
    @PostMapping("/products/{id}/reject")
    @Operation(
        summary = "Reject product",
        description = "Reject a product and change its status to REJECTED with the specified reason"
    )
    @ApiResponses(value = [
        ApiResponse(responseCode = "200", description = "Product rejected successfully"),
        ApiResponse(responseCode = "400", description = "Invalid request"),
        ApiResponse(responseCode = "403", description = "User is not a moderator"),
        ApiResponse(responseCode = "404", description = "Product not found"),
        ApiResponse(responseCode = "500", description = "Internal server error")
    ])
    fun rejectProduct(
        @RequestHeader("X-User-Id")
        @Parameter(hidden = true)
        moderatorId: Long,

        @PathVariable
        @Parameter(description = "Product ID", example = "100")
        id: Long,

        @Valid
        @RequestBody
        request: RejectProductRequest
    ): Mono<ResponseEntity<ModerationResultResponse>> {
        return moderationService.rejectProduct(moderatorId, id, request.reason)
            .map { ResponseEntity.status(HttpStatus.OK).body(it) }
    }
    
    @PostMapping("/bulk")
    @Operation(
        summary = "Bulk moderation",
        description = "Approve or reject multiple products at once"
    )
    @ApiResponses(value = [
        ApiResponse(responseCode = "200", description = "Products processed successfully"),
        ApiResponse(responseCode = "400", description = "Invalid request"),
        ApiResponse(responseCode = "403", description = "User is not a moderator"),
        ApiResponse(responseCode = "500", description = "Internal server error")
    ])
    fun bulkModerate(
        @RequestHeader("X-User-Id")
        @Parameter(hidden = true)
        moderatorId: Long,

        @Valid
        @RequestBody
        request: BulkModerationRequest
    ): Flux<ModerationResultResponse> {
        return moderationService.bulkModerate(moderatorId, request)
    }

    // ========== ИСТОРИЯ ==========

    @GetMapping("/history")
    @Operation(
        summary = "Get moderation history",
        description = "Retrieve the history of all moderation actions performed by a specific moderator"
    )
    @ApiResponses(value = [
        ApiResponse(responseCode = "200", description = "History retrieved successfully"),
        ApiResponse(responseCode = "403", description = "User is not a moderator")
    ])
    fun getModerationHistory(
        @RequestHeader("X-User-Id")
        @Parameter(hidden = true)
        moderatorId: Long
    ): Flux<ru.itmo.market.domain.model.entity.ModerationAction> {
        return moderationService.getModerationHistory(moderatorId)
    }
    
    @GetMapping("/products/{id}/history")
    @Operation(
        summary = "Get product moderation history",
        description = "Retrieve the history of all moderation actions for a specific product"
    )
    @ApiResponses(value = [
        ApiResponse(responseCode = "200", description = "History retrieved successfully"),
        ApiResponse(responseCode = "403", description = "User is not a moderator"),
        ApiResponse(responseCode = "404", description = "Product not found")
    ])
    fun getProductModerationHistory(
        @PathVariable
        @Parameter(description = "Product ID", example = "100")
        id: Long
    ): Flux<ru.itmo.market.domain.model.entity.ModerationAudit> {
        return moderationService.getProductModerationHistory(id)
    }
}
