package ru.itmo.productservice.domain.entity

import jakarta.persistence.*
import jakarta.validation.constraints.*
import org.hibernate.annotations.CreationTimestamp
import org.hibernate.annotations.UpdateTimestamp
import java.math.BigDecimal
import java.time.LocalDateTime
import ru.itmo.productservice.domain.enums.ProductStatus

@Entity
@Table(name = "products", indexes = [
    Index(name = "idx_products_shop_id", columnList = "shop_id"),
    Index(name = "idx_products_seller_id", columnList = "seller_id"),
    Index(name = "idx_products_status", columnList = "status")
])
data class Product(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,
    
    @field:NotBlank(message = "Product name is required")
    @field:Size(max = 255)
    @Column(nullable = false)
    val name: String,
    
    @Column(columnDefinition = "TEXT")
    val description: String? = null,
    
    @field:DecimalMin(value = "0.01", message = "Price must be greater than 0")
    @Column(nullable = false, precision = 19, scale = 2)
    val price: BigDecimal,
    
    @Column(name = "image_url")
    val imageUrl: String? = null,
    
    @Column(name = "shop_id", nullable = false)
    val shopId: Long,
    
    @Column(name = "seller_id", nullable = false)
    val sellerId: Long,
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    val status: ProductStatus = ProductStatus.PENDING,
    
    @Column(name = "rejection_reason", columnDefinition = "TEXT")
    val rejectionReason: String? = null,
    
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    val createdAt: LocalDateTime = LocalDateTime.now(),
    
    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    val updatedAt: LocalDateTime = LocalDateTime.now()
)
