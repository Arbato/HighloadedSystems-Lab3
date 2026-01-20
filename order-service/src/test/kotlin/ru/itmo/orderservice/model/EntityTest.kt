package ru.itmo.orderservice.model

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.DisplayName
import ru.itmo.orderservice.domain.model.entity.Order
import ru.itmo.orderservice.domain.model.entity.OrderItem
import ru.itmo.orderservice.domain.model.enums.OrderStatus
import java.math.BigDecimal
import java.time.LocalDateTime

@DisplayName("Entity Tests")
class EntityTest {

    // ==================== Order Entity Tests ====================

    @Test
    @DisplayName("Order entity has correct default values")
    fun testOrderDefaultValues() {
        val order = Order(userId = 1L)

        assert(order.id == 0L)
        assert(order.totalPrice == BigDecimal.ZERO)
        assert(order.status == OrderStatus.CART)
        assert(order.deliveryAddress == null)
    }

    @Test
    @DisplayName("Order entity copy works correctly")
    fun testOrderCopy() {
        val order = Order(
            id = 1L,
            userId = 1L,
            totalPrice = BigDecimal("100.00"),
            status = OrderStatus.CART
        )

        val copied = order.copy(status = OrderStatus.PENDING, deliveryAddress = "123 Main St")

        assert(copied.id == 1L)
        assert(copied.userId == 1L)
        assert(copied.totalPrice == BigDecimal("100.00"))
        assert(copied.status == OrderStatus.PENDING)
        assert(copied.deliveryAddress == "123 Main St")
    }

    @Test
    @DisplayName("Order entity equality works")
    fun testOrderEquality() {
        val now = LocalDateTime.now()
        val order1 = Order(id = 1L, userId = 1L, createdAt = now, updatedAt = now)
        val order2 = Order(id = 1L, userId = 1L, createdAt = now, updatedAt = now)
        val order3 = Order(id = 2L, userId = 1L, createdAt = now, updatedAt = now)

        assert(order1 == order2)
        assert(order1 != order3)
    }

    @Test
    @DisplayName("Order entity hashCode works")
    fun testOrderHashCode() {
        val now = LocalDateTime.now()
        val order1 = Order(id = 1L, userId = 1L, createdAt = now, updatedAt = now)
        val order2 = Order(id = 1L, userId = 1L, createdAt = now, updatedAt = now)

        assert(order1.hashCode() == order2.hashCode())
    }

    @Test
    @DisplayName("Order can have all statuses")
    fun testOrderStatuses() {
        OrderStatus.entries.forEach { status ->
            val order = Order(userId = 1L, status = status)
            assert(order.status == status)
        }
    }

    // ==================== OrderItem Entity Tests ====================

    @Test
    @DisplayName("OrderItem entity has required fields")
    fun testOrderItemRequiredFields() {
        val item = OrderItem(
            orderId = 1L,
            productId = 1L,
            quantity = 2,
            price = BigDecimal("50.00")
        )

        assert(item.id == 0L)
        assert(item.orderId == 1L)
        assert(item.productId == 1L)
        assert(item.quantity == 2)
        assert(item.price == BigDecimal("50.00"))
    }

    @Test
    @DisplayName("OrderItem entity copy works correctly")
    fun testOrderItemCopy() {
        val item = OrderItem(
            id = 1L,
            orderId = 1L,
            productId = 1L,
            quantity = 2,
            price = BigDecimal("50.00")
        )

        val copied = item.copy(quantity = 5)

        assert(copied.id == 1L)
        assert(copied.orderId == 1L)
        assert(copied.productId == 1L)
        assert(copied.quantity == 5)
        assert(copied.price == BigDecimal("50.00"))
    }

    @Test
    @DisplayName("OrderItem entity equality works")
    fun testOrderItemEquality() {
        val now = LocalDateTime.now()
        val item1 = OrderItem(id = 1L, orderId = 1L, productId = 1L, quantity = 1, price = BigDecimal("10.00"), createdAt = now)
        val item2 = OrderItem(id = 1L, orderId = 1L, productId = 1L, quantity = 1, price = BigDecimal("10.00"), createdAt = now)
        val item3 = OrderItem(id = 2L, orderId = 1L, productId = 1L, quantity = 1, price = BigDecimal("10.00"), createdAt = now)

        assert(item1 == item2)
        assert(item1 != item3)
    }

    @Test
    @DisplayName("OrderItem entity hashCode works")
    fun testOrderItemHashCode() {
        val now = LocalDateTime.now()
        val item1 = OrderItem(id = 1L, orderId = 1L, productId = 1L, quantity = 1, price = BigDecimal("10.00"), createdAt = now)
        val item2 = OrderItem(id = 1L, orderId = 1L, productId = 1L, quantity = 1, price = BigDecimal("10.00"), createdAt = now)

        assert(item1.hashCode() == item2.hashCode())
    }

    // ==================== OrderStatus Enum Tests ====================

    @Test
    @DisplayName("OrderStatus enum has all expected values")
    fun testOrderStatusValues() {
        val statuses = OrderStatus.entries.map { it.name }

        assert("CART" in statuses)
        assert("PENDING" in statuses)
        assert("PROCESSING" in statuses)
        assert("SHIPPED" in statuses)
        assert("DELIVERED" in statuses)
        assert("CANCELED" in statuses)
    }

    @Test
    @DisplayName("OrderStatus can be converted from string")
    fun testOrderStatusFromString() {
        assert(OrderStatus.valueOf("CART") == OrderStatus.CART)
        assert(OrderStatus.valueOf("PENDING") == OrderStatus.PENDING)
        assert(OrderStatus.valueOf("DELIVERED") == OrderStatus.DELIVERED)
    }
}
