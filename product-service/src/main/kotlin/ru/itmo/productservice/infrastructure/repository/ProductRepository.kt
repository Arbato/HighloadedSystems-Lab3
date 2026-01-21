package ru.itmo.productservice.infrastructure.repository

import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import ru.itmo.productservice.domain.entity.Product
import ru.itmo.productservice.domain.enums.ProductStatus

@Repository
interface ProductRepository : JpaRepository<Product, Long> {
    
    /**
     * Найти все товары с определенным статусом (для пагинации)
     */
    fun findAllByStatus(status: ProductStatus, pageable: Pageable): Page<Product>
    
    /**
     * Найти все товары магазина (для пагинации)
     */
    fun findAllByShopId(shopId: Long, pageable: Pageable): Page<Product>
    
    /**
     * Поиск одобренных товаров по ключевым словам
     */
    @Query("""
        SELECT p FROM Product p 
        WHERE p.status = 'APPROVED' 
        AND (LOWER(p.name) LIKE LOWER(CONCAT('%', :keyword, '%'))
             OR LOWER(p.description) LIKE LOWER(CONCAT('%', :keyword, '%')))
        ORDER BY p.createdAt DESC
    """)
    fun searchApprovedProducts(
        @Param("keyword") keyword: String,
        pageable: Pageable
    ): Page<Product>
    
    /**
     * Подсчет товаров магазина
     */
    fun countByShopId(shopId: Long): Long
    
    /**
     * Найти товар на модерацию по ID
     */
    @Query("SELECT p FROM Product p WHERE p.id = :id AND p.status = 'PENDING'")
    fun findPendingById(@Param("id") id: Long): Product?
    
    /**
     * Получить все товары на модерацию
     */
    fun findAllByStatus(status: ProductStatus): List<Product>
}
