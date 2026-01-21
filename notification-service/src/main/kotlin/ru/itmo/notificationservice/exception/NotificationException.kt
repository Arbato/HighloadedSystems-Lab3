package ru.itmo.notificationservice.exception

class NotificationException(message: String, cause: Throwable? = null) : 
    DomainException(message, cause)