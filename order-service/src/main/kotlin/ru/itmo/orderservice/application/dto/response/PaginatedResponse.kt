package ru.itmo.orderservice.application.dto.response

/**
 * PaginatedResponse для универсального использования
 */
data class PaginatedResponse<T>(
    val data: List<T>,
    val page: Int,
    val pageSize: Int,
    val totalElements: Long,
    val totalPages: Int
)