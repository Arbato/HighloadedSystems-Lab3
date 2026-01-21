package ru.itmo.notificationservice.controller

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import reactor.core.publisher.Mono
import ru.itmo.notificationservice.model.dto.request.SubscriptionRequest
import ru.itmo.notificationservice.model.dto.response.NotificationListResponse
import ru.itmo.notificationservice.model.dto.response.NotificationResponse
import ru.itmo.notificationservice.model.dto.response.SubscriptionResponse
import ru.itmo.notificationservice.service.application.NotificationService

@RestController
@RequestMapping("/api/notifications")
@Tag(name = "Notifications", description = "Notification management API")
class NotificationController(
    private val notificationService: NotificationService
) {

    /**
     * Получить все уведомления текущего пользователя
     */
    @GetMapping("/user/{userId}")
    @Operation(
        summary = "Get user notifications",
        description = "Returns all notifications for the user, paginated"
    )
    fun getUserNotifications(
        @PathVariable userId: Long,
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "20") pageSize: Int,
        @RequestHeader("X-User-Id")
        @Parameter(hidden = true)
        requestingUserId: Long
    ): Mono<ResponseEntity<NotificationListResponse>> {
        return notificationService.getUserNotifications(userId, page, pageSize)
            .map { ResponseEntity.ok(it) }
    }

    /**
     * Получить только непрочитанные уведомления
     */
    @GetMapping("/user/{userId}/unread")
    @Operation(summary = "Get unread notifications")
    fun getUnreadNotifications(
        @PathVariable userId: Long,
        @RequestHeader("X-User-Id")
        @Parameter(hidden = true)
        requestingUserId: Long
    ): Mono<ResponseEntity<List<NotificationResponse>>> {
        return notificationService.getUnreadNotifications(userId)
            .map { ResponseEntity.ok(it) }
    }

    /**
     * Отметить уведомление как прочитанное
     */
    @PutMapping("/{notificationId}/read")
    @Operation(summary = "Mark notification as read")
    fun markAsRead(
        @PathVariable notificationId: Long,
        @RequestHeader("X-User-Id")
        @Parameter(hidden = true)
        userId: Long
    ): Mono<ResponseEntity<NotificationResponse>> {
        return notificationService.markAsRead(notificationId, userId)
            .map { ResponseEntity.ok(it) }
    }

    /**
     * Отметить все уведомления как прочитанные
     */
    @PutMapping("/user/{userId}/read-all")
    @Operation(summary = "Mark all notifications as read")
    fun markAllAsRead(
        @PathVariable userId: Long,
        @RequestHeader("X-User-Id")
        @Parameter(hidden = true)
        requestingUserId: Long
    ): Mono<ResponseEntity<Void>> {
        return notificationService.markAllAsRead(userId)
            .then(Mono.just(ResponseEntity.ok().build()))
    }

    /**
     * Удалить уведомление
     */
    @DeleteMapping("/{notificationId}")
    @Operation(summary = "Delete notification")
    fun deleteNotification(
        @PathVariable notificationId: Long,
        @RequestHeader("X-User-Id")
        @Parameter(hidden = true)
        userId: Long
    ): Mono<ResponseEntity<Void>> {
        return notificationService.deleteNotification(notificationId, userId)
            .then(Mono.just(ResponseEntity.noContent().build()))
    }

    /**
     * Управление подписками на события
     */
    @PostMapping("/subscriptions")
    @Operation(summary = "Create subscription")
    fun createSubscription(
        @RequestHeader("X-User-Id")
        @Parameter(hidden = true)
        userId: Long,
        @Valid @RequestBody request: SubscriptionRequest
    ): Mono<ResponseEntity<SubscriptionResponse>> {
        return notificationService.createSubscription(userId, request)
            .map { ResponseEntity.ok(it) }
    }

    /**
     * Получить подписки пользователя
     */
    @GetMapping("/subscriptions/user/{userId}")
    @Operation(summary = "Get user subscriptions")
    fun getUserSubscriptions(
        @PathVariable userId: Long,
        @RequestHeader("X-User-Id")
        @Parameter(hidden = true)
        requestingUserId: Long
    ): Mono<ResponseEntity<List<SubscriptionResponse>>> {
        return notificationService.getUserSubscriptions(userId)
            .map { ResponseEntity.ok(it) }
    }

    /**
     * Удалить подписку
     */
    @DeleteMapping("/subscriptions/{subscriptionId}")
    @Operation(summary = "Delete subscription")
    fun deleteSubscription(
        @PathVariable subscriptionId: Long,
        @RequestHeader("X-User-Id")
        @Parameter(hidden = true)
        userId: Long
    ): Mono<ResponseEntity<Void>> {
        return notificationService.deleteSubscription(subscriptionId, userId)
            .then(Mono.just(ResponseEntity.noContent().build()))
    }
}
