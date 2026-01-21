package ru.itmo.fileservice.model.entity

import jakarta.persistence.*
import org.hibernate.annotations.CreationTimestamp
import org.hibernate.annotations.UpdateTimestamp
import ru.itmo.fileservice.model.enums.FileStatus
import ru.itmo.fileservice.model.enums.FileType
import java.time.LocalDateTime

@Entity
@Table(name = "file_metadata", indexes = [
    Index(name = "idx_user_id", columnList = "user_id"),
    Index(name = "idx_file_key", columnList = "file_key", unique = true),
    Index(name = "idx_status", columnList = "status"),
    Index(name = "idx_created_at", columnList = "created_at")
])
data class FileMetadata(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0L,

    val userId: Long,                        // ID пользователя (внешний ключ)

    @Column(unique = true)
    val fileKey: String,                     // Уникальный ключ файла (UUID или hash)

    val fileName: String,                    // Оригинальное имя файла

    @Enumerated(EnumType.STRING)
    val fileType: FileType,                  // Тип файла (IMAGE, DOCUMENT, VIDEO и т.д.)

    val contentType: String,                 // MIME type (image/jpeg, application/pdf и т.д.)

    val fileSizeBytes: Long,                 // Размер файла в байтах

    @Enumerated(EnumType.STRING)
    val status: FileStatus = FileStatus.PENDING,  // Статус обработки

    val storagePath: String,                 // Путь в S3 или локальном хранилище

    val checksumMd5: String? = null,        // MD5 хеш для проверки целостности

    val virusScanStatus: String = "CLEAN",   // Статус проверки на вирусы

    val isPublic: Boolean = false,           // Публичный или приватный доступ

    val expiresAt: LocalDateTime? = null,    // Время истечения (опционально)

    val metadata: String? = null,            // JSON с дополнительными данными

    @CreationTimestamp
    val createdAt: LocalDateTime = LocalDateTime.now(),

    @UpdateTimestamp
    val updatedAt: LocalDateTime = LocalDateTime.now(),

    val downloadCount: Long = 0L,            // Количество загрузок

    val lastDownloadedAt: LocalDateTime? = null  // Когда последний раз скачан
)
