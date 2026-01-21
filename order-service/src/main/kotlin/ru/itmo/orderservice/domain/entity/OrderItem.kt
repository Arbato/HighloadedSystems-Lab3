package ru.itmo.orderservice.domain.entity

import jakarta.persistence.*
import jakarta.validation.constraints.Min
import org.hibernate.annotations.CreationTimestamp
import java.math.BigDecimal
import java.time.LocalDateTime

@Entity
@Table(name = "order_items", uniqueConstraints = [
    UniqueConstraint(columnNames = ["order_id", "product_id"])
])
data class OrderItem(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,
    
    @Column(name = "order_id", nullable = false)
    val orderId: Long,
    
    @Column(name = "product_id", nullable = false)
    val productId: Long,
    
    @field:Min(value = 1, message = "Quantity must be at least 1")
    @Column(nullable = false)
    val quantity: Int,
    
    @Column(nullable = false, precision = 19, scale = 2)
    val price: BigDecimal,
    
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    val createdAt: LocalDateTime = LocalDateTime.now()
)
