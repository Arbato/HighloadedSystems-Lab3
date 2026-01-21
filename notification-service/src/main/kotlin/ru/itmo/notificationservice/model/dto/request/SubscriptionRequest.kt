package ru.itmo.notificationservice.model.dto.request

import jakarta.validation.constraints.NotBlank

data class SubscriptionRequest(
    @field:NotBlank(message = "Event type is required")
    val eventType: String,
    
    @field:NotBlank(message = "Channel is required")
    val channel: String = "IN_APP"
)
