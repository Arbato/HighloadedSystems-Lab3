package ru.itmo.orderservice.domain.model.enums

enum class OrderStatus {
    CART,        // Корзина (активная)
    PENDING,     // Оформленный заказ (ожидающий обработки)
    PROCESSING,  // В обработке
    SHIPPED,     // Отправлен
    DELIVERED,   // Доставлен
    CANCELED     // Отменен
}
