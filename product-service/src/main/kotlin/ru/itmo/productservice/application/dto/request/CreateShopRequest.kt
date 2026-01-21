package ru.itmo.productservice.application.dto.request

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size

data class CreateShopRequest(
    @field:NotBlank(message = "Shop name is required")
    @field:Size(max = 200)
    val name: String,
    
    val description: String? = null,
    
    val avatarUrl: String? = null
)
