package ru.itmo.orderservice.domain.entity

import jakarta.persistence.*
import org.hibernate.annotations.CreationTimestamp
import org.hibernate.annotations.UpdateTimestamp
import java.math.BigDecimal
import java.time.LocalDateTime
import ru.itmo.orderservice.domain.enums.OrderStatus

@Entity
@Table(name = "orders", indexes = [
    Index(name = "idx_orders_user_id", columnList = "user_id"),
    Index(name = "idx_orders_status", columnList = "status"),
    Index(name = "idx_orders_user_status", columnList = "user_id, status")
])
data class Order(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,
    
    @Column(name = "user_id", nullable = false)
    val userId: Long,
    
    @Column(nullable = false, precision = 19, scale = 2)
    val totalPrice: BigDecimal = BigDecimal.ZERO,
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    val status: OrderStatus = OrderStatus.CART,
    
    @Column(name = "delivery_address", columnDefinition = "TEXT")
    val deliveryAddress: String? = null,
    
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    val createdAt: LocalDateTime = LocalDateTime.now(),
    
    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    val updatedAt: LocalDateTime = LocalDateTime.now()
)
