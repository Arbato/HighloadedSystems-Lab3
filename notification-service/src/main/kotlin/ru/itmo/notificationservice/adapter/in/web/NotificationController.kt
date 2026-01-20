package ru.itmo.notificationservice.adapter.`in`.web

import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import reactor.core.publisher.Mono
import ru.itmo.notificationservice.application.service.NotificationService
import ru.itmo.notificationservice.domain.model.dto.response.NotificationResponse
import ru.itmo.notificationservice.domain.model.dto.response.PaginatedNotificationResponse

@RestController
@RequestMapping("/api/notifications")
class NotificationController(
    private val notificationService: NotificationService
) {

    @GetMapping
    fun getUserNotifications(
        @RequestHeader("X-User-Id") userId: Long,
        @RequestParam(defaultValue = "1") page: Int,
        @RequestParam(defaultValue = "20") pageSize: Int
    ): Mono<ResponseEntity<PaginatedNotificationResponse>> {
        return notificationService.getUserNotifications(userId, page, pageSize)
            .map { ResponseEntity.ok(it) }
    }

    @GetMapping("/unread")
    fun getUnreadNotifications(
        @RequestHeader("X-User-Id") userId: Long
    ): Mono<ResponseEntity<List<NotificationResponse>>> {
        return notificationService.getUnreadNotifications(userId)
            .collectList()
            .map { ResponseEntity.ok(it) }
    }

    @GetMapping("/unread/count")
    fun getUnreadCount(
        @RequestHeader("X-User-Id") userId: Long
    ): Mono<ResponseEntity<Map<String, Long>>> {
        return notificationService.getUnreadCount(userId)
            .map { ResponseEntity.ok(mapOf("count" to it)) }
    }

    @PutMapping("/{notificationId}/read")
    fun markAsRead(
        @RequestHeader("X-User-Id") userId: Long,
        @PathVariable notificationId: Long
    ): Mono<ResponseEntity<NotificationResponse>> {
        return notificationService.markAsRead(notificationId, userId)
            .map { ResponseEntity.ok(it) }
            .defaultIfEmpty(ResponseEntity.notFound().build())
    }
}
