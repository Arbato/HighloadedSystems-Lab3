package ru.itmo.orderservice.domain.repository

import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.DisplayName
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager
import org.springframework.data.domain.PageRequest
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.TestPropertySource
import ru.itmo.orderservice.domain.model.entity.Order
import ru.itmo.orderservice.domain.model.entity.OrderItem
import ru.itmo.orderservice.domain.model.enums.OrderStatus
import java.math.BigDecimal

@DataJpaTest
@ActiveProfiles("test")
@TestPropertySource(properties = [
    "spring.cloud.config.enabled=false",
    "spring.cloud.config.import-check.enabled=false",
    "eureka.client.enabled=false"
])
@DisplayName("Order Repository Tests")
class OrderRepositoryTest {

    @Autowired
    private lateinit var orderRepository: OrderRepository

    @Autowired
    private lateinit var orderItemRepository: OrderItemRepository

    @Autowired
    private lateinit var entityManager: TestEntityManager

    @BeforeEach
    fun setUp() {
        orderItemRepository.deleteAll()
        orderRepository.deleteAll()
        entityManager.flush()
    }

    @Test
    @DisplayName("Should find cart by user ID and status")
    fun testFindByUserIdAndStatus() {
        val cart = Order(
            userId = 1L,
            status = OrderStatus.CART,
            totalPrice = BigDecimal.ZERO
        )
        orderRepository.save(cart)
        entityManager.flush()

        val result = orderRepository.findByUserIdAndStatus(1L, OrderStatus.CART)

        assert(result.isPresent)
        assert(result.get().userId == 1L)
        assert(result.get().status == OrderStatus.CART)
    }

    @Test
    @DisplayName("Should return empty when no cart exists")
    fun testFindByUserIdAndStatusNotFound() {
        val result = orderRepository.findByUserIdAndStatus(999L, OrderStatus.CART)

        assert(result.isEmpty)
    }

    @Test
    @DisplayName("Should find orders by user ID excluding status")
    fun testFindAllByUserIdAndStatusNot() {
        val cart = Order(userId = 1L, status = OrderStatus.CART)
        val pending = Order(userId = 1L, status = OrderStatus.PENDING, deliveryAddress = "Address")
        val delivered = Order(userId = 1L, status = OrderStatus.DELIVERED, deliveryAddress = "Address")

        orderRepository.saveAll(listOf(cart, pending, delivered))
        entityManager.flush()

        val pageable = PageRequest.of(0, 20)
        val result = orderRepository.findAllByUserIdAndStatusNot(1L, OrderStatus.CART, pageable)

        assert(result.content.size == 2)
        assert(result.content.none { it.status == OrderStatus.CART })
    }

    @Test
    @DisplayName("Should return empty page when no orders")
    fun testFindAllByUserIdAndStatusNotEmpty() {
        val cart = Order(userId = 1L, status = OrderStatus.CART)
        orderRepository.save(cart)
        entityManager.flush()

        val pageable = PageRequest.of(0, 20)
        val result = orderRepository.findAllByUserIdAndStatusNot(1L, OrderStatus.CART, pageable)

        assert(result.content.isEmpty())
    }

    @Test
    @DisplayName("Should find order by ID and user ID")
    fun testFindByIdAndUserId() {
        val order = Order(
            userId = 1L,
            status = OrderStatus.PENDING,
            deliveryAddress = "123 Main St"
        )
        val saved = orderRepository.save(order)
        entityManager.flush()

        val result = orderRepository.findByIdAndUserId(saved.id, 1L)

        assert(result.isPresent)
        assert(result.get().id == saved.id)
    }

    @Test
    @DisplayName("Should not find order for different user")
    fun testFindByIdAndUserIdDifferentUser() {
        val order = Order(
            userId = 1L,
            status = OrderStatus.PENDING,
            deliveryAddress = "123 Main St"
        )
        val saved = orderRepository.save(order)
        entityManager.flush()

        val result = orderRepository.findByIdAndUserId(saved.id, 999L)

        assert(result.isEmpty)
    }

    @Test
    @DisplayName("Should paginate orders correctly")
    fun testPagination() {
        // Create 5 orders
        repeat(5) { i ->
            orderRepository.save(Order(
                userId = 1L,
                status = OrderStatus.PENDING,
                deliveryAddress = "Address $i"
            ))
        }
        entityManager.flush()

        val page1 = orderRepository.findAllByUserIdAndStatusNot(
            1L, OrderStatus.CART, PageRequest.of(0, 2)
        )
        val page2 = orderRepository.findAllByUserIdAndStatusNot(
            1L, OrderStatus.CART, PageRequest.of(1, 2)
        )

        assert(page1.content.size == 2)
        assert(page2.content.size == 2)
        assert(page1.totalElements == 5L)
        assert(page1.totalPages == 3)
    }

    @Test
    @DisplayName("Should save order with all fields")
    fun testSaveOrderWithAllFields() {
        val order = Order(
            userId = 1L,
            totalPrice = BigDecimal("199.99"),
            status = OrderStatus.SHIPPED,
            deliveryAddress = "456 Oak Ave"
        )

        val saved = orderRepository.save(order)
        entityManager.flush()
        entityManager.clear()

        val found = orderRepository.findById(saved.id)

        assert(found.isPresent)
        assert(found.get().totalPrice.compareTo(BigDecimal("199.99")) == 0)
        assert(found.get().status == OrderStatus.SHIPPED)
        assert(found.get().deliveryAddress == "456 Oak Ave")
    }

    @Test
    @DisplayName("Should update order status")
    fun testUpdateOrderStatus() {
        val order = Order(
            userId = 1L,
            status = OrderStatus.PENDING,
            deliveryAddress = "Address"
        )
        val saved = orderRepository.save(order)
        entityManager.flush()

        val updated = saved.copy(status = OrderStatus.PROCESSING)
        orderRepository.save(updated)
        entityManager.flush()
        entityManager.clear()

        val found = orderRepository.findById(saved.id)
        assert(found.get().status == OrderStatus.PROCESSING)
    }
}
