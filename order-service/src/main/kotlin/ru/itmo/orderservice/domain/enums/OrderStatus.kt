package ru.itmo.orderservice.domain.enums

enum class OrderStatus {
    CART,        // Корзина (активная)
    PENDING,     // Оформленный заказ (ожидающий обработки)
    PROCESSING,  // В обработке
    SHIPPED,     // Отправлен
    DELIVERED,   // Доставлен
    CANCELED     // Отменен
}
