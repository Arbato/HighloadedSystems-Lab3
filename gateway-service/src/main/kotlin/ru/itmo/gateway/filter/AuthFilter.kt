package ru.itmo.gateway.filter

import org.springframework.cloud.gateway.filter.GatewayFilterChain
import org.springframework.cloud.gateway.filter.GlobalFilter
import org.springframework.core.Ordered
import org.springframework.http.HttpStatus
import org.springframework.http.server.reactive.ServerHttpRequest
import org.springframework.stereotype.Component
import org.springframework.web.server.ServerWebExchange
import reactor.core.publisher.Mono
import ru.itmo.gateway.service.JwtValidationService

@Component
class AuthFilter(
    private val jwtValidationService: JwtValidationService
) : GlobalFilter, Ordered {

    companion object {
        private val PUBLIC_ENDPOINTS = listOf(
            "/user-service/api/auth/register",
            "/user-service/api/auth/login",
            "/actuator/health",
            "/swagger-ui",
            "/v1/api-docs",
            "/v3/api-docs",
            "/webjars"
        )

        private val ANONYMOUS_READ_ENDPOINTS = listOf(
            Pair("/product-service/api/products", "GET"),
            Pair("/product-service/api/shops", "GET")
        )
    }

    override fun filter(exchange: ServerWebExchange, chain: GatewayFilterChain): Mono<Void> {
        val request = exchange.request
        val path = request.uri.path
        val method = request.method.name()

        if (isPublicEndpoint(path)) {
            return chain.filter(exchange)
        }

        if (isAnonymousReadAllowed(path, method)) {
            return chain.filter(exchange)
        }

        val authHeader = request.headers.getFirst("Authorization")

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return unauthorized(exchange)
        }

        val token = authHeader.substring(7)
        val claims = jwtValidationService.validateToken(token)

        if (claims == null) {
            return unauthorized(exchange)
        }

        val userId = jwtValidationService.getUserId(claims)
        val roles = jwtValidationService.getRoles(claims)

        val modifiedRequest: ServerHttpRequest = request.mutate()
            .header("X-User-Id", userId)
            .header("X-User-Roles", roles.joinToString(","))
            .build()

        return chain.filter(exchange.mutate().request(modifiedRequest).build())
    }

    override fun getOrder(): Int = -100

    private fun isPublicEndpoint(path: String): Boolean {
        return PUBLIC_ENDPOINTS.any { path.startsWith(it) }
    }

    private fun isAnonymousReadAllowed(path: String, method: String): Boolean {
        return ANONYMOUS_READ_ENDPOINTS.any { (endpointPath, allowedMethod) ->
            path.startsWith(endpointPath) && method == allowedMethod
        }
    }

    private fun unauthorized(exchange: ServerWebExchange): Mono<Void> {
        exchange.response.statusCode = HttpStatus.UNAUTHORIZED
        return exchange.response.setComplete()
    }
}
