package ru.itmo.productservice.domain.repository

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import ru.itmo.productservice.domain.model.entity.Shop
import java.util.*

@Repository
interface ShopRepository : JpaRepository<Shop, Long> {
    
    /**
     * Проверить существование магазина по ID продавца
     */
    fun existsBySellerId(sellerId: Long): Boolean
    
    /**
     * Найти магазин по ID продавца
     */
    fun findBySellerId(sellerId: Long): Optional<Shop>
}
