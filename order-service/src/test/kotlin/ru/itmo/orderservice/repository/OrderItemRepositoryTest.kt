package ru.itmo.orderservice.domain.repository

import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.DisplayName
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager
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
@DisplayName("OrderItem Repository Tests")
class OrderItemRepositoryTest {

    @Autowired
    private lateinit var orderRepository: OrderRepository

    @Autowired
    private lateinit var orderItemRepository: OrderItemRepository

    @Autowired
    private lateinit var entityManager: TestEntityManager

    private lateinit var testOrder: Order

    @BeforeEach
    fun setUp() {
        orderItemRepository.deleteAll()
        orderRepository.deleteAll()

        testOrder = Order(
            userId = 1L,
            status = OrderStatus.CART
        )
        testOrder = orderRepository.save(testOrder)
        entityManager.flush()
    }

    @Test
    @DisplayName("Should find item by order ID and product ID")
    fun testFindByOrderIdAndProductId() {
        val item = OrderItem(
            orderId = testOrder.id,
            productId = 1L,
            quantity = 2,
            price = BigDecimal("100.00")
        )
        orderItemRepository.save(item)
        entityManager.flush()

        val result = orderItemRepository.findByOrderIdAndProductId(testOrder.id, 1L)

        assert(result.isPresent)
        assert(result.get().productId == 1L)
        assert(result.get().quantity == 2)
    }

    @Test
    @DisplayName("Should return empty when item not found")
    fun testFindByOrderIdAndProductIdNotFound() {
        val result = orderItemRepository.findByOrderIdAndProductId(testOrder.id, 999L)

        assert(result.isEmpty)
    }

    @Test
    @DisplayName("Should find all items by order ID")
    fun testFindAllByOrderId() {
        val item1 = OrderItem(
            orderId = testOrder.id,
            productId = 1L,
            quantity = 2,
            price = BigDecimal("100.00")
        )
        val item2 = OrderItem(
            orderId = testOrder.id,
            productId = 2L,
            quantity = 1,
            price = BigDecimal("50.00")
        )
        orderItemRepository.saveAll(listOf(item1, item2))
        entityManager.flush()

        val result = orderItemRepository.findAllByOrderId(testOrder.id)

        assert(result.size == 2)
    }

    @Test
    @DisplayName("Should return empty list when no items")
    fun testFindAllByOrderIdEmpty() {
        val result = orderItemRepository.findAllByOrderId(testOrder.id)

        assert(result.isEmpty())
    }

    @Test
    @DisplayName("Should delete all items by order ID")
    fun testDeleteByOrderId() {
        val item1 = OrderItem(
            orderId = testOrder.id,
            productId = 1L,
            quantity = 2,
            price = BigDecimal("100.00")
        )
        val item2 = OrderItem(
            orderId = testOrder.id,
            productId = 2L,
            quantity = 1,
            price = BigDecimal("50.00")
        )
        orderItemRepository.saveAll(listOf(item1, item2))
        entityManager.flush()

        orderItemRepository.deleteByOrderId(testOrder.id)
        entityManager.flush()

        val result = orderItemRepository.findAllByOrderId(testOrder.id)
        assert(result.isEmpty())
    }

    @Test
    @DisplayName("Should save item with all fields")
    fun testSaveItemWithAllFields() {
        val item = OrderItem(
            orderId = testOrder.id,
            productId = 1L,
            quantity = 3,
            price = BigDecimal("99.99")
        )

        val saved = orderItemRepository.save(item)
        entityManager.flush()
        entityManager.clear()

        val found = orderItemRepository.findById(saved.id)

        assert(found.isPresent)
        assert(found.get().quantity == 3)
        assert(found.get().price.compareTo(BigDecimal("99.99")) == 0)
    }

    @Test
    @DisplayName("Should update item quantity")
    fun testUpdateItemQuantity() {
        val item = OrderItem(
            orderId = testOrder.id,
            productId = 1L,
            quantity = 2,
            price = BigDecimal("100.00")
        )
        val saved = orderItemRepository.save(item)
        entityManager.flush()

        val updated = saved.copy(quantity = 5)
        orderItemRepository.save(updated)
        entityManager.flush()
        entityManager.clear()

        val found = orderItemRepository.findById(saved.id)
        assert(found.get().quantity == 5)
    }

    @Test
    @DisplayName("Should not find items from different order")
    fun testItemsIsolatedByOrder() {
        val order2 = orderRepository.save(Order(userId = 2L, status = OrderStatus.CART))
        entityManager.flush()

        val item1 = OrderItem(
            orderId = testOrder.id,
            productId = 1L,
            quantity = 1,
            price = BigDecimal("100.00")
        )
        val item2 = OrderItem(
            orderId = order2.id,
            productId = 1L,
            quantity = 1,
            price = BigDecimal("100.00")
        )
        orderItemRepository.saveAll(listOf(item1, item2))
        entityManager.flush()

        val result = orderItemRepository.findAllByOrderId(testOrder.id)
        assert(result.size == 1)
        assert(result[0].orderId == testOrder.id)
    }
}
