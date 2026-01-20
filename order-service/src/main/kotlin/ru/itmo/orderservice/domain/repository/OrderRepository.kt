package ru.itmo.orderservice.domain.repository

import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import ru.itmo.orderservice.domain.model.entity.Order
import ru.itmo.orderservice.domain.model.enums.OrderStatus
import java.util.Optional

@Repository
interface OrderRepository : JpaRepository<Order, Long> {
    
    /**
     * Найти корзину пользователя (активный заказ со статусом CART)
     */
    fun findByUserIdAndStatus(userId: Long, status: OrderStatus): Optional<Order>
    
    /**
     * Получить все заказы пользователя (кроме корзины)
     */
    fun findAllByUserIdAndStatusNot(
        userId: Long,
        status: OrderStatus,
        pageable: Pageable
    ): Page<Order>
    
    /**
     * Найти заказ по ID и ID пользователя (для проверки прав)
     */
    fun findByIdAndUserId(orderId: Long, userId: Long): Optional<Order>
    
    /**
     * Получить заказ по ID
     */
    override fun findById(id: Long): Optional<Order>
}
