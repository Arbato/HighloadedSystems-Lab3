package ru.itmo.productservice.application.dto.request

data class UpdateShopRequest(
    val name: String? = null,
    val description: String? = null,
    val avatarUrl: String? = null
)
