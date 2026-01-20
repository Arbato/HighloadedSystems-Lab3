package ru.itmo.productservice.domain.model.dto.request

import jakarta.validation.constraints.*
import java.math.BigDecimal

data class CreateProductRequest(
    @field:NotBlank(message = "Product name is required")
    @field:Size(max = 255)
    val name: String,
    
    val description: String? = null,
    
    @field:DecimalMin(value = "0.01", message = "Price must be greater than 0")
    val price: BigDecimal,
    
    val imageUrl: String? = null,
    
    @field:NotNull(message = "Shop ID is required")
    val shopId: Long
)
