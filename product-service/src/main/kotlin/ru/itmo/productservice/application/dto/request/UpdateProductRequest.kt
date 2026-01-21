package ru.itmo.productservice.application.dto.request

import java.math.BigDecimal

data class UpdateProductRequest(
    val name: String? = null,
    val description: String? = null,
    val price: BigDecimal? = null,
    val imageUrl: String? = null
)
