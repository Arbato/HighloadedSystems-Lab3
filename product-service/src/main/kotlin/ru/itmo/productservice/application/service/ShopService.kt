package ru.itmo.productservice.application.service

import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import ru.itmo.productservice.infrastructure.client.KafkaRpcClient
import ru.itmo.productservice.adapters.exception.BadRequestException
import ru.itmo.productservice.adapters.exception.ForbiddenException
import ru.itmo.productservice.adapters.exception.ResourceNotFoundException
import ru.itmo.productservice.application.dto.request.CreateShopRequest
import ru.itmo.productservice.application.dto.request.UpdateShopRequest
import ru.itmo.productservice.application.dto.response.PaginatedResponse
import ru.itmo.productservice.application.dto.response.ProductResponse
import ru.itmo.productservice.application.dto.response.ShopResponse
import ru.itmo.productservice.domain.entity.Shop
import ru.itmo.productservice.domain.entity.Product
import ru.itmo.productservice.infrastructure.repository.ProductRepository
import ru.itmo.productservice.infrastructure.repository.ShopRepository

@Service
class ShopService(
    private val shopRepository: ShopRepository,
    private val productRepository: ProductRepository,
    private val kafkaRpcClient: KafkaRpcClient
) {
    
    /**
     * Получить все магазины
     */
    fun getAllShops(page: Int, pageSize: Int): PaginatedResponse<ShopResponse> {
        if (page < 1 || pageSize < 1) {
            throw BadRequestException("Page and pageSize must be greater than 0")
        }
        
        val pageable = PageRequest.of(page - 1, pageSize)
        val shopPage = shopRepository.findAll(pageable)
        
        return PaginatedResponse(
            data = shopPage.content.map { it.toResponse(kafkaRpcClient, productRepository) },
            page = page,
            pageSize = pageSize,
            totalElements = shopPage.totalElements,
            totalPages = shopPage.totalPages
        )
    }
    
    /**
     * Получить магазин по ID
     */
    fun getShopById(shopId: Long): ShopResponse {
        if (shopId <= 0) {
            throw BadRequestException("Invalid shop ID: $shopId")
        }
        
        val shop = shopRepository.findById(shopId)
            .orElseThrow { ResourceNotFoundException("Shop not found with ID: $shopId") }
        
        return shop.toResponse(kafkaRpcClient, productRepository)
    }
    
    /**
     * Создать магазин (только один магазин на продавца)
     */
    @Transactional
    fun createShop(
        sellerId: Long,
        request: CreateShopRequest
    ): ShopResponse {
        if (sellerId <= 0) {
            throw BadRequestException("Invalid seller ID: $sellerId")
        }
        
        if (request.name.isBlank()) {
            throw BadRequestException("Shop name cannot be empty")
        }
        
        // Проверяем что у продавца еще нет магазина
        if (shopRepository.existsBySellerId(sellerId)) {
            throw BadRequestException("Seller already has a shop")
        }
        
        // Проверяем что пользователь существует
        //userServiceClient.getUserById(sellerId)
        
        try {
            kafkaRpcClient.getUserById(sellerId)
        } catch (e : Exception) {
            throw ResourceNotFoundException("User with ID $sellerId not found")
        }

        val shop = Shop(
            name = request.name,
            description = request.description,
            avatarUrl = request.avatarUrl,
            sellerId = sellerId
        )
        
        val savedShop = shopRepository.save(shop)
        return savedShop.toResponse(kafkaRpcClient, productRepository)
    }
    
    /**
     * Обновить магазин (только владелец)
     */
    @Transactional
    fun updateShop(
        shopId: Long,
        sellerId: Long,
        request: UpdateShopRequest
    ): ShopResponse {
        if (shopId <= 0 || sellerId <= 0) {
            throw BadRequestException("Invalid shop ID or seller ID")
        }
        
        val shop = shopRepository.findById(shopId)
            .orElseThrow { ResourceNotFoundException("Shop not found with ID: $shopId") }
        
        // Проверяем что это владелец магазина
        if (shop.sellerId != sellerId) {
            throw ForbiddenException("Only shop owner can update the shop")
        }
        
        val updatedShop = shop.copy(
            name = request.name ?: shop.name,
            description = request.description ?: shop.description,
            avatarUrl = request.avatarUrl ?: shop.avatarUrl
        )
        
        val savedShop = shopRepository.save(updatedShop)
        return savedShop.toResponse(kafkaRpcClient, productRepository)
    }
    
    /**
     * Получить товары магазина
     */
    fun getShopProducts(
        shopId: Long,
        page: Int,
        pageSize: Int
    ): PaginatedResponse<ProductResponse> {
        if (shopId <= 0) {
            throw BadRequestException("Invalid shop ID: $shopId")
        }
        
        if (page < 1 || pageSize < 1) {
            throw BadRequestException("Page and pageSize must be greater than 0")
        }
        
        if (!shopRepository.existsById(shopId)) {
            throw ResourceNotFoundException("Shop not found with ID: $shopId")
        }
        
        val pageable = PageRequest.of(page - 1, pageSize)
        val productPage = productRepository.findAllByShopId(shopId, pageable)
        
        return PaginatedResponse(
            data = productPage.content.map { it.toResponse() },
            page = page,
            pageSize = pageSize,
            totalElements = productPage.totalElements,
            totalPages = productPage.totalPages
        )
    }
    
    /**
     * Вспомогательный метод для преобразования Entity в Response
     */
    private fun Shop.toResponse(
        kafkaRpcClient: KafkaRpcClient,
        productRepository: ProductRepository
    ): ShopResponse {
        val user = try {
            kafkaRpcClient.getUserById(this.sellerId)
        } catch (e: Exception) {
            null
        }
        
        val productsCount = productRepository.countByShopId(this.id)
        
        return ShopResponse(
            id = this.id,
            name = this.name,
            description = this.description,
            avatarUrl = this.avatarUrl,
            sellerId = this.sellerId,
            sellerName = user?.let { "${it.firstName} ${it.lastName}" },
            productsCount = productsCount,
            createdAt = this.createdAt,
            updatedAt = this.updatedAt
        )
    }
    
    /**
     * Преобразовать Product Entity в Response
     */
    private fun Product.toResponse(): ProductResponse {
        return ProductResponse(
            id = this.id,
            name = this.name,
            description = this.description,
            price = this.price,
            imageUrl = this.imageUrl,
            shopId = this.shopId,
            sellerId = this.sellerId,
            status = this.status.name,
            rejectionReason = this.rejectionReason,
            createdAt = this.createdAt,
            updatedAt = this.updatedAt
        )
    }
}
