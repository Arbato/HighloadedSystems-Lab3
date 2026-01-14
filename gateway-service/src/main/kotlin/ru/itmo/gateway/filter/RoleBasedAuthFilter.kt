package ru.itmo.gateway.filter

import org.springframework.cloud.gateway.filter.GatewayFilterChain
import org.springframework.cloud.gateway.filter.GlobalFilter
import org.springframework.core.Ordered
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Component
import org.springframework.web.server.ServerWebExchange
import reactor.core.publisher.Mono

@Component
class RoleBasedAuthFilter : GlobalFilter, Ordered {

    companion object {
        private val SELLER_ENDPOINTS = listOf(
            Pair("/product-service/api/shops", "POST"),
            Pair("/product-service/api/products", "POST")
        )

        private val MODERATOR_ENDPOINTS = listOf(
            "/moderation-service/api/moderation"
        )

        private val MODERATOR_PRODUCT_ENDPOINTS = listOf(
            Pair("/product-service/api/products/pending", "GET"),
            Triple("/product-service/api/products/", "/approve", "POST"),
            Triple("/product-service/api/products/", "/reject", "POST")
        )

        private val ADMIN_ENDPOINTS = listOf(
            "/user-service/api/auth/roles/assign"
        )
    }

    override fun filter(exchange: ServerWebExchange, chain: GatewayFilterChain): Mono<Void> {
        val request = exchange.request
        val path = request.uri.path
        val method = request.method.name()

        val rolesHeader = request.headers.getFirst("X-User-Roles")
        val roles = rolesHeader?.split(",")?.map { it.trim() } ?: emptyList()

        if (rolesHeader == null) {
            return chain.filter(exchange)
        }

        if (isAdminEndpoint(path)) {
            if (!roles.contains("ADMIN")) {
                return forbidden(exchange)
            }
        }

        if (isModeratorEndpoint(path, method)) {
            if (!roles.contains("MODERATOR") && !roles.contains("ADMIN")) {
                return forbidden(exchange)
            }
        }

        if (isSellerEndpoint(path, method)) {
            if (!roles.contains("SELLER") && !roles.contains("ADMIN")) {
                return forbidden(exchange)
            }
        }

        return chain.filter(exchange)
    }

    override fun getOrder(): Int = -90

    private fun isSellerEndpoint(path: String, method: String): Boolean {
        return SELLER_ENDPOINTS.any { (endpointPath, allowedMethod) ->
            path.startsWith(endpointPath) && method == allowedMethod
        }
    }

    private fun isModeratorEndpoint(path: String, method: String): Boolean {
        if (MODERATOR_ENDPOINTS.any { path.startsWith(it) }) {
            return true
        }

        return MODERATOR_PRODUCT_ENDPOINTS.any { endpoint ->
            when (endpoint) {
                is Pair<*, *> -> path.startsWith(endpoint.first as String) && method == endpoint.second
                is Triple<*, *, *> -> {
                    val prefix = endpoint.first as String
                    val suffix = endpoint.second as String
                    val expectedMethod = endpoint.third as String
                    path.startsWith(prefix) && path.contains(suffix) && method == expectedMethod
                }
                else -> false
            }
        }
    }

    private fun isAdminEndpoint(path: String): Boolean {
        return ADMIN_ENDPOINTS.any { path.startsWith(it) }
    }

    private fun forbidden(exchange: ServerWebExchange): Mono<Void> {
        exchange.response.statusCode = HttpStatus.FORBIDDEN
        return exchange.response.setComplete()
    }
}
