package ru.itmo.market.domain.model.dto.request

import jakarta.validation.Valid
import jakarta.validation.constraints.NotEmpty

data class BulkModerationRequest(
    @field:NotEmpty(message = "Product IDs list cannot be empty")
    val productIds: List<Long>,
    
    val action: String, // APPROVE or REJECT
    
    val reason: String? = null // Required for REJECT
)