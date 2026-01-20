package ru.itmo.productservice.service

import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import ru.itmo.productservice.client.UserServiceClient
import ru.itmo.productservice.client.KafkaRpcClient
import ru.itmo.productservice.exception.BadRequestException
import ru.itmo.productservice.exception.ForbiddenException
import ru.itmo.productservice.exception.ResourceNotFoundException
import ru.itmo.productservice.model.dto.request.CreateProductRequest
import ru.itmo.productservice.model.dto.request.UpdateProductRequest
import ru.itmo.productservice.model.dto.response.PaginatedResponse
import ru.itmo.productservice.model.dto.response.ProductResponse
import ru.itmo.productservice.model.enums.ProductStatus
import ru.itmo.productservice.model.enums.UserRole
import ru.itmo.productservice.model.entity.Product
import ru.itmo.productservice.repository.ProductRepository
import ru.itmo.productservice.repository.ShopRepository
import java.math.BigDecimal

@Service
class ProductService(
    private val productRepository: ProductRepository,
    private val shopRepository: ShopRepository,
    private val kafkaRpcClient: KafkaRpcClient
) {
    
    /**
     * Получить все одобренные товары (пагинация)
     */
    fun getApprovedProducts(page: Int, pageSize: Int): PaginatedResponse<ProductResponse> {
        // Валидация
        if (page < 1 || pageSize < 1) {
            throw BadRequestException("Page and pageSize must be greater than 0")
        }
        
        val pageable = PageRequest.of(page - 1, pageSize)
        val productPage = productRepository.findAllByStatus(ProductStatus.APPROVED, pageable)
        
        return PaginatedResponse(
            data = productPage.content.map { it.toResponse() },
            page = page,
            pageSize = pageSize,
            totalElements = productPage.totalElements,
            totalPages = productPage.totalPages
        )
    }
    
    /**
     * Получить товары магазина (пагинация)
     */
    fun getProductsByShopId(shopId: Long, page: Int, pageSize: Int): PaginatedResponse<ProductResponse> {
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
     * Поиск товаров по ключевым словам
     */
    fun searchProducts(keyword: String, page: Int, pageSize: Int): PaginatedResponse<ProductResponse> {
        if (page < 1 || pageSize < 1) {
            throw BadRequestException("Page and pageSize must be greater than 0")
        }
        
        if (keyword.isBlank()) {
            throw BadRequestException("Search keyword cannot be empty")
        }
        
        val pageable = PageRequest.of(page - 1, pageSize)
        val productPage = productRepository.searchApprovedProducts(keyword, pageable)
        
        return PaginatedResponse(
            data = productPage.content.map { it.toResponse() },
            page = page,
            pageSize = pageSize,
            totalElements = productPage.totalElements,
            totalPages = productPage.totalPages
        )
    }
    
    /**
     * Получить товар по ID
     */
    fun getProductById(productId: Long): ProductResponse {
        if (productId <= 0) {
            throw BadRequestException("Invalid product ID: $productId")
        }
        
        val product = productRepository.findById(productId)
            .orElseThrow { ResourceNotFoundException("Product not found with ID: $productId") }
        
        return product.toResponse()
    }
    
    /**
     * Создать новый товар (только для продавца/модератора)
     */
    @Transactional
    fun createProduct(request: CreateProductRequest, sellerId: Long): ProductResponse {
        if (sellerId <= 0) {
            throw BadRequestException("Invalid seller ID: $sellerId")
        }
        
        if (request.name.isBlank()) {
            throw BadRequestException("Product name cannot be empty")
        }
        
        // Проверяем что магазин принадлежит продавцу
        val shop = shopRepository.findById(request.shopId)
            .orElseThrow { ResourceNotFoundException("Shop not found with ID: ${request.shopId}") }
        
        if (shop.sellerId != sellerId) {
            throw ForbiddenException("Only shop owner can add products")
        }
        
        val product = Product(
            name = request.name,
            description = request.description,
            price = request.price,
            imageUrl = request.imageUrl,
            shopId = request.shopId,
            sellerId = sellerId,
            status = ProductStatus.PENDING  // По умолчанию на модерации
        )
        
        val savedProduct = productRepository.save(product)
        return savedProduct.toResponse()
    }
    
    /**
     * Обновить товар (только для модератора)
     */
    @Transactional
    fun updateProduct(
        productId: Long,
        userId: Long,
        request: UpdateProductRequest
    ): ProductResponse {
        if (productId <= 0 || userId <= 0) {
            throw BadRequestException("Invalid product ID or user ID")
        }
        
        val product = productRepository.findById(productId)
            .orElseThrow { ResourceNotFoundException("Product not found with ID: $productId") }
        
        // Проверяем что пользователь - модератор
        val user = try {
            kafkaRpcClient.getUserById(userId).user
        } catch (e : Exception) {
            throw ResourceNotFoundException("User not found with ID $userId")
        }

        if (!user!!.roles.contains(UserRole.MODERATOR.name)) {
            throw ForbiddenException("Only moderators can update products")
        }
        
        val updatedProduct = product.copy(
            name = request.name ?: product.name,
            description = request.description ?: product.description,
            price = request.price ?: product.price,
            imageUrl = request.imageUrl ?: product.imageUrl
        )
        
        val savedProduct = productRepository.save(updatedProduct)
        return savedProduct.toResponse()
    }
    
    /**
     * Одобрить товар (только для модератора)
     */
    @Transactional
    fun approveProduct(productId: Long, moderatorId: Long): ProductResponse {
        if (productId <= 0 || moderatorId <= 0) {
            throw BadRequestException("Invalid product ID or moderator ID")
        }
        
        val product = productRepository.findById(productId)
            .orElseThrow { ResourceNotFoundException("Product not found with ID: $productId") }
        
        // Проверяем что пользователь - модератор
        val user = try {
            kafkaRpcClient.getUserById(moderatorId).user
        } catch (e : Exception) {
            throw ResourceNotFoundException("User not found with ID $moderatorId")
        }
        
        if (!user!!.roles.contains(UserRole.MODERATOR.name)) {
            throw ForbiddenException("Only moderators can approve products")
        }
        
        // Проверяем статус
        if (product.status != ProductStatus.PENDING) {
            throw IllegalStateException("Only PENDING products can be approved")
        }
        
        val approvedProduct = product.copy(status = ProductStatus.APPROVED)
        val savedProduct = productRepository.save(approvedProduct)
        return savedProduct.toResponse()
    }
    
    /**
     * Отклонить товар (только для модератора)
     */
    @Transactional
    fun rejectProduct(productId: Long, moderatorId: Long, reason: String): ProductResponse {
        if (productId <= 0 || moderatorId <= 0) {
            throw BadRequestException("Invalid product ID or moderator ID")
        }
        
        if (reason.isBlank()) {
            throw BadRequestException("Rejection reason cannot be empty")
        }
        
        val product = productRepository.findById(productId)
            .orElseThrow { ResourceNotFoundException("Product not found with ID: $productId") }
        
        // Проверяем что пользователь - модератор
        val user = try {
            kafkaRpcClient.getUserById(moderatorId).user
        } catch (e : Exception) {
            throw ResourceNotFoundException("User not found with ID $moderatorId")
        }

        if (!user!!.roles.contains(UserRole.MODERATOR.name)) {
            throw ForbiddenException("Only moderators can reject products")
        }
        
        // Проверяем статус
        if (product.status != ProductStatus.PENDING) {
            throw IllegalStateException("Only PENDING products can be rejected")
        }
        
        val rejectedProduct = product.copy(
            status = ProductStatus.REJECTED,
            rejectionReason = reason
        )
        val savedProduct = productRepository.save(rejectedProduct)
        return savedProduct.toResponse()
    }
    
    /**
     * Удалить товар (только для модератора)
     */
    @Transactional
    fun deleteProduct(productId: Long, userId: Long) {
        if (productId <= 0 || userId <= 0) {
            throw BadRequestException("Invalid product ID or user ID")
        }
        
        val product = productRepository.findById(productId)
            .orElseThrow { ResourceNotFoundException("Product not found with ID: $productId") }
        
        // Проверяем что пользователь - модератор или владелец
        val user = try {
            kafkaRpcClient.getUserById(userId).user
        } catch (e : Exception) {
            throw ResourceNotFoundException("User not found with ID $userId")
        }

        if (product.sellerId != userId && !user!!.roles.contains(UserRole.MODERATOR.name)) {
            throw ForbiddenException("You can only delete your own products")
        }
        
        productRepository.deleteById(productId)
    }
    
    /**
     * Подсчет товаров магазина
     */
    fun countProductsByShopId(shopId: Long): Long {
        return productRepository.countByShopId(shopId)
    }
    
    /**
     * Проверить существование товара
     */
    fun existsById(productId: Long): Boolean {
        return productRepository.existsById(productId)
    }
    
    /**
     * Получить товар на модерацию
     */
    fun getPendingProductById(productId: Long): ProductResponse {
        if (productId <= 0) {
            throw BadRequestException("Invalid product ID: $productId")
        }
        
        val product = productRepository.findPendingById(productId)
            ?: throw ResourceNotFoundException("Pending product not found with ID: $productId")
        
        return product.toResponse()
    }
    
    /**
     * Получить все товары на модерацию
     */
    fun getPendingProducts(page: Int, pageSize: Int): PaginatedResponse<ProductResponse> {
        if (page < 1 || pageSize < 1) {
            throw BadRequestException("Page and pageSize must be greater than 0")
        }
        
        val pageable = PageRequest.of(page - 1, pageSize)
        val productPage = productRepository.findAllByStatus(ProductStatus.PENDING, pageable)
        
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
