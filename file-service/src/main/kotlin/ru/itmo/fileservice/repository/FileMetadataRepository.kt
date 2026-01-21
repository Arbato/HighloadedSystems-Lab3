package ru.itmo.fileservice.repository

import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import ru.itmo.fileservice.model.entity.FileMetadata
import ru.itmo.fileservice.model.enums.FileStatus
import ru.itmo.fileservice.model.enums.FileType
import java.time.LocalDateTime
import java.util.*

@Repository
interface FileMetadataRepository : JpaRepository<FileMetadata, Long> {

    // Найти файл по ключу
    fun findByFileKey(fileKey: String): Optional<FileMetadata>

    // Найти файл по ID и проверить принадлежность пользователю
    fun findByIdAndUserId(fileId: Long, userId: Long): Optional<FileMetadata>

    // Получить все файлы пользователя (исключая удалённые)
    fun findAllByUserIdAndStatusNot(
        userId: Long,
        status: FileStatus,
        pageable: Pageable
    ): Page<FileMetadata>

    // Получить файлы пользователя по типу
    fun findAllByUserIdAndFileType(
        userId: Long,
        fileType: FileType,
        pageable: Pageable
    ): Page<FileMetadata>

    // Получить файлы пользователя по статусу
    fun findAllByUserIdAndStatus(
        userId: Long,
        status: FileStatus,
        pageable: Pageable
    ): Page<FileMetadata>

    // Найти истекшие файлы
    @Query("""
        SELECT f FROM FileMetadata f 
        WHERE f.expiresAt IS NOT NULL 
        AND f.expiresAt < :now 
        AND f.status != 'DELETED'
    """)
    fun findExpiredFiles(now: LocalDateTime): List<FileMetadata>

    // Получить публичные файлы
    fun findAllByIsPublicAndStatusNot(
        isPublic: Boolean,
        status: FileStatus,
        pageable: Pageable
    ): Page<FileMetadata>

    // Статистика хранилища пользователя
    @Query("""
        SELECT SUM(f.fileSizeBytes) FROM FileMetadata f 
        WHERE f.userId = :userId AND f.status != 'DELETED'
    """)
    fun getUserStorageUsedBytes(userId: Long): Long?

    // Кол-во файлов пользователя
    @Query("""
        SELECT COUNT(f) FROM FileMetadata f 
        WHERE f.userId = :userId AND f.status != 'DELETED'
    """)
    fun countUserFiles(userId: Long): Long

    // Наиболее скачиваемые файлы
    @Query("""
        SELECT f FROM FileMetadata f 
        WHERE f.userId = :userId AND f.status != 'DELETED'
        ORDER BY f.downloadCount DESC
        LIMIT :limit
    """)
    fun findTopDownloadedFiles(userId: Long, limit: Int): List<FileMetadata>

    // Файлы, требующие сканирования вирусов
    fun findAllByStatus(status: FileStatus): List<FileMetadata>

    // Удалить все файлы пользователя (soft delete)
    fun deleteByUserId(userId: Long)
}
