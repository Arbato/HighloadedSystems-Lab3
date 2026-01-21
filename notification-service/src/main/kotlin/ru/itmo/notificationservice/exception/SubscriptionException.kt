package ru.itmo.notificationservice.exception

class SubscriptionException(message: String, cause: Throwable? = null) : 
    DomainException(message, cause)