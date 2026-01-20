package ru.itmo.orderservice.domain.repository

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import ru.itmo.orderservice.domain.model.entity.OrderItem
import java.util.Optional

@Repository
interface OrderItemRepository : JpaRepository<OrderItem, Long> {
    
    /**
     * Найти товар в заказе
     */
    fun findByOrderIdAndProductId(orderId: Long, productId: Long): Optional<OrderItem>
    
    /**
     * Получить все товары в заказе
     */
    fun findAllByOrderId(orderId: Long): List<OrderItem>
    
    /**
     * Удалить все товары из заказа
     */
    fun deleteByOrderId(orderId: Long)
}
