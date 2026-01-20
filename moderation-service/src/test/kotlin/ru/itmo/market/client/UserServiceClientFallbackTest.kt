package ru.itmo.market.client

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.assertThrows
import ru.itmo.market.adapter.exception.ServiceUnavailableException

@DisplayName("UserServiceClientFallback Tests")
class UserServiceClientFallbackTest {

    private val fallback = UserServiceClientFallback()

    @Test
    @DisplayName("getUserById should throw ServiceUnavailableException")
    fun testGetUserByIdThrowsException() {
        val exception = assertThrows<ServiceUnavailableException> {
            fallback.getUserById(1L)
        }
        assert(exception.message?.contains("1") == true)
    }

    @Test
    @DisplayName("getUserById should include user ID in message")
    fun testGetUserByIdMessageContainsId() {
        val exception = assertThrows<ServiceUnavailableException> {
            fallback.getUserById(999L)
        }
        assert(exception.message?.contains("999") == true)
    }
}
