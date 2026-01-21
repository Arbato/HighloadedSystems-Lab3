package ru.itmo.fileservice.service

import mu.KotlinLogging
import org.springframework.cache.annotation.CacheEvict
import org.springframework.cache.annotation.Cacheable
import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import ru.itmo.fileservice.exception.*
import ru.itmo.fileservice.model.dto.request.UploadFileRequest
import ru.itmo.fileservice.model.dto.request.DeleteFileRequest
import ru.itmo.fileservice.model.dto.request.UpdateFileMetadataRequest
import ru.itmo.fileservice.model.dto.response.*
import ru.itmo.fileservice.model.entity.FileMetadata
import ru.itmo.fileservice.model.enums.FileStatus
import ru.itmo.fileservice.model.enums.FileType
import ru.itmo.fileservice.repository.FileMetadataRepository
import java.math.BigDecimal
import java.time.LocalDateTime
import java.util.*

private val logger = KotlinLogging.logger {}

@Service
@Transactional
class FileService(
    private val fileMetadataRepository: FileMetadataRepository,
    private val fileStorageService: FileStorageService,
    private val fileValidationService: FileValidationService
) {

    companion object {
        private const val MAX_FILE_SIZE_MB = 500
        private const val MAX_FILES_PER_USER = 10000
        private const val CACHE_KEY_FILE = "file"
    }

    /**
     * Загрузить файл
     */
    @Transactional
    @CacheEvict(value = [CACHE_KEY_FILE], allEntries = true)
    fun uploadFile(userId: Long, request: UploadFileRequest): FileUploadResponse {
        logger.info { "Uploading file for user $userId: ${request.fileName}" }

        // Валидация
        fileValidationService.validateUserId(userId)
        fileValidationService.validateUploadRequest(request, MAX_FILE_SIZE_MB)
        checkUserQuota(userId, request.fileSizeBytes)

        try {
            // Генерируем уникальный ключ файла
            val fileKey = UUID.randomUUID().toString()

            // Определяем тип файла
            val fileType = determineFileType(request.fileName, request.contentType)

            // Вычисляем MD5 хеш
            val checksumMd5 = fileValidationService.calculateMd5(request.fileContent)

            // Сохраняем файл в хранилище
            val storagePath = fileStorageService.saveFile(
                fileKey = fileKey,
                fileName = request.fileName,
                content = request.fileContent,
                contentType = request.contentType
            )

            // Создаём метаданные файла
            val expiresAt = request.expiresInMinutes?.let {
                LocalDateTime.now().plusMinutes(it.toLong())
            }

            val fileMetadata = FileMetadata(
                userId = userId,
                fileKey = fileKey,
                fileName = request.fileName,
                fileType = fileType,
                contentType = request.contentType,
                fileSizeBytes = request.fileSizeBytes,
                status = FileStatus.ACTIVE,
                storagePath = storagePath,
                checksumMd5 = checksumMd5,
                isPublic = request.isPublic,
                expiresAt = expiresAt,
                metadata = request.metadata?.let { serializeMetadata(it) }
            )

            val saved = fileMetadataRepository.save(fileMetadata)

            logger.info { "File uploaded successfully: fileKey=$fileKey, userId=$userId" }

            return FileUploadResponse(
                fileId = saved.id,
                fileKey = saved.fileKey,
                fileName = saved.fileName,
                fileType = saved.fileType.name,
                fileSizeBytes = saved.fileSizeBytes,
                contentType = saved.contentType,
                status = saved.status.name,
                downloadUrl = generateDownloadUrl(fileKey),
                expiresAt = saved.expiresAt,
                uploadedAt = saved.createdAt,
                message = "File uploaded successfully"
            )

        } catch (e: Exception) {
            logger.error(e) { "Error uploading file for user $userId" }
            throw FileUploadException("Failed to upload file: ${e.message}", e)
        }
    }

    /**
     * Скачать файл
     */
    @Transactional
    fun downloadFile(userId: Long, fileId: Long): FileDownloadResponse {
        logger.info { "Downloading file $fileId for user $userId" }

        // Валидация
        fileValidationService.validateUserId(userId)
        fileValidationService.validateFileId(fileId)

        // Получить метаданные
        val fileMetadata = fileMetadataRepository.findByIdAndUserId(fileId, userId)
            .orElseThrow { FileNotFoundException("File not found: $fileId") }

        // Проверить статус
        if (fileMetadata.status != FileStatus.ACTIVE) {
            throw FileNotFoundException("File is not available for download: ${fileMetadata.status}")
        }

        // Проверить истечение
        if (fileMetadata.expiresAt != null && LocalDateTime.now().isAfter(fileMetadata.expiresAt)) {
            throw FileNotFoundException("File has expired")
        }

        try {
            // Получить содержимое файла
            val content = fileStorageService.getFile(fileMetadata.storagePath)

            // Обновить счётчик загрузок
            val updated = fileMetadata.copy(
                downloadCount = fileMetadata.downloadCount + 1,
                lastDownloadedAt = LocalDateTime.now()
            )
            fileMetadataRepository.save(updated)

            logger.info { "File downloaded successfully: fileId=$fileId, userId=$userId" }

            return FileDownloadResponse(
                fileId = fileMetadata.id,
                fileName = fileMetadata.fileName,
                contentType = fileMetadata.contentType,
                fileSizeBytes = fileMetadata.fileSizeBytes,
                content = content,
                checksumMd5 = fileMetadata.checksumMd5,
                createdAt = fileMetadata.createdAt
            )

        } catch (e: Exception) {
            logger.error(e) { "Error downloading file $fileId for user $userId" }
            throw StorageException("Failed to download file: ${e.message}", e)
        }
    }

    /**
     * Получить метаданные файла
     */
    @Cacheable(value = [CACHE_KEY_FILE], key = "#fileId + '_' + #userId")
    fun getFileMetadata(userId: Long, fileId: Long): FileMetadataResponse {
        logger.info { "Getting metadata for file $fileId, user $userId" }

        fileValidationService.validateUserId(userId)
        fileValidationService.validateFileId(fileId)

        val fileMetadata = fileMetadataRepository.findByIdAndUserId(fileId, userId)
            .orElseThrow { FileNotFoundException("File not found: $fileId") }

        return toFileMetadataResponse(fileMetadata)
    }

    /**
     * Получить список файлов пользователя
     */
    fun getUserFiles(userId: Long, page: Int = 1, pageSize: Int = 20): PaginatedFilesResponse {
        logger.info { "Getting files for user $userId, page=$page, pageSize=$pageSize" }

        fileValidationService.validateUserId(userId)
        fileValidationService.validatePagination(page, pageSize)

        val pageable = PageRequest.of(page - 1, pageSize)
        val filesPage = fileMetadataRepository.findAllByUserIdAndStatusNot(
            userId,
            FileStatus.DELETED,
            pageable
        )

        return PaginatedFilesResponse(
            data = filesPage.content.map { toFileMetadataResponse(it) },
            page = page,
            pageSize = pageSize,
            totalElements = filesPage.totalElements,
            totalPages = filesPage.totalPages,
            hasNext = filesPage.hasNext(),
            hasPrevious = filesPage.hasPrevious()
        )
    }

    /**
     * Получить файлы по типу
     */
    fun getUserFilesByType(userId: Long, fileType: FileType, page: Int = 1, pageSize: Int = 20): PaginatedFilesResponse {
        logger.info { "Getting files of type $fileType for user $userId" }

        fileValidationService.validateUserId(userId)

        val pageable = PageRequest.of(page - 1, pageSize)
        val filesPage = fileMetadataRepository.findAllByUserIdAndFileType(
            userId,
            fileType,
            pageable
        )

        return PaginatedFilesResponse(
            data = filesPage.content.map { toFileMetadataResponse(it) },
            page = page,
            pageSize = pageSize,
            totalElements = filesPage.totalElements,
            totalPages = filesPage.totalPages,
            hasNext = filesPage.hasNext(),
            hasPrevious = filesPage.hasPrevious()
        )
    }

    /**
     * Удалить файл (soft delete)
     */
    @CacheEvict(value = [CACHE_KEY_FILE], allEntries = true)
    fun deleteFile(userId: Long, request: DeleteFileRequest): String {
        logger.info { "Deleting file ${request.fileId} for user $userId" }

        fileValidationService.validateUserId(userId)
        fileValidationService.validateFileId(request.fileId)

        val fileMetadata = fileMetadataRepository.findByIdAndUserId(request.fileId, userId)
            .orElseThrow { FileNotFoundException("File not found: ${request.fileId}") }

        return if (request.softDelete) {
            // Soft delete - меняем статус
            val updated = fileMetadata.copy(status = FileStatus.DELETED)
            fileMetadataRepository.save(updated)
            logger.info { "File soft deleted: fileId=${request.fileId}, userId=$userId" }
            "File marked as deleted"
        } else {
            // Hard delete - удаляем физически
            try {
                fileStorageService.deleteFile(fileMetadata.storagePath)
                fileMetadataRepository.delete(fileMetadata)
                logger.info { "File hard deleted: fileId=${request.fileId}, userId=$userId" }
                "File permanently deleted"
            } catch (e: Exception) {
                logger.error(e) { "Error hard deleting file ${request.fileId}" }
                throw StorageException("Failed to delete file: ${e.message}", e)
            }
        }
    }

    /**
     * Обновить метаданные файла
     */
    @CacheEvict(value = [CACHE_KEY_FILE], allEntries = true)
    fun updateFileMetadata(userId: Long, request: UpdateFileMetadataRequest): FileMetadataResponse {
        logger.info { "Updating metadata for file ${request.fileId}, user $userId" }

        fileValidationService.validateUserId(userId)

        val fileMetadata = fileMetadataRepository.findByIdAndUserId(request.fileId, userId)
            .orElseThrow { FileNotFoundException("File not found: ${request.fileId}") }

        var updated = fileMetadata

        request.fileName?.let {
            updated = updated.copy(fileName = it)
        }

        request.isPublic?.let {
            updated = updated.copy(isPublic = it)
        }

        request.metadata?.let {
            updated = updated.copy(metadata = serializeMetadata(it))
        }

        val saved = fileMetadataRepository.save(updated)

        logger.info { "Metadata updated for file ${request.fileId}" }

        return toFileMetadataResponse(saved)
    }

    /**
     * Получить статистику пользователя
     */
    fun getUserStatistics(userId: Long): FileStatisticsResponse {
        logger.info { "Getting statistics for user $userId" }

        fileValidationService.validateUserId(userId)

        val totalFiles = fileMetadataRepository.countUserFiles(userId)
        val totalStorageBytes = fileMetadataRepository.getUserStorageUsedBytes(userId) ?: 0L
        val topDownloadedFiles = fileMetadataRepository.findTopDownloadedFiles(userId, 5)
        val allUserFiles = fileMetadataRepository.findAllByUserIdAndStatusNot(
            userId,
            FileStatus.DELETED,
            PageRequest.of(0, 5)
        )

        // Группировать по типам файлов
        val filesByType = FileType.values().associate { fileType ->
            val count = fileMetadataRepository.findAllByUserIdAndFileType(
                userId,
                fileType,
                PageRequest.of(0, 1)
            ).totalElements
            fileType.name to count
        }.filterValues { it > 0 }

        // Группировать по статусам
        val filesByStatus = FileStatus.values().associate { status ->
            val count = fileMetadataRepository.findAllByUserIdAndStatus(
                userId,
                status,
                PageRequest.of(0, 1)
            ).totalElements
            status.name to count
        }.filterValues { it > 0 }

        return FileStatisticsResponse(
            totalFiles = totalFiles,
            totalStorageBytes = totalStorageBytes,
            totalStorageMB = BigDecimal(totalStorageBytes).divide(BigDecimal(1024 * 1024)),
            averageFileSizeBytes = if (totalFiles > 0) totalStorageBytes / totalFiles else 0L,
            filesByType = filesByType,
            filesByStatus = filesByStatus,
            mostDownloadedFiles = topDownloadedFiles.map { toFileMetadataResponse(it) },
            lastUploadedFiles = allUserFiles.content.map { toFileMetadataResponse(it) }
        )
    }

    /**
     * Удалить истекшие файлы (cronJob)
     */
    @CacheEvict(value = [CACHE_KEY_FILE], allEntries = true)
    fun deleteExpiredFiles(): String {
        logger.info { "Checking and deleting expired files" }

        val expiredFiles = fileMetadataRepository.findExpiredFiles(LocalDateTime.now())

        if (expiredFiles.isEmpty()) {
            logger.info { "No expired files found" }
            return "No expired files"
        }

        expiredFiles.forEach { fileMetadata ->
            try {
                fileStorageService.deleteFile(fileMetadata.storagePath)
                fileMetadataRepository.delete(fileMetadata)
                logger.info { "Deleted expired file: ${fileMetadata.fileKey}" }
            } catch (e: Exception) {
                logger.error(e) { "Error deleting expired file ${fileMetadata.fileKey}" }
            }
        }

        logger.info { "Deleted ${expiredFiles.size} expired files" }
        return "Deleted ${expiredFiles.size} expired files"
    }

    // ==================== Private Methods ====================

    private fun determineFileType(fileName: String, contentType: String): FileType {
        return when {
            contentType.startsWith("image/") -> FileType.IMAGE
            contentType.startsWith("video/") -> FileType.VIDEO
            contentType.startsWith("audio/") -> FileType.AUDIO
            contentType.contains("pdf") || contentType.contains("document") || contentType.contains("sheet") -> FileType.DOCUMENT
            contentType.contains("zip") || contentType.contains("rar") || contentType.contains("7z") || contentType.contains("archive") -> FileType.ARCHIVE
            fileName.endsWith(".java") || fileName.endsWith(".kt") || fileName.endsWith(".py") || fileName.endsWith(".js") -> FileType.CODE
            else -> FileType.OTHER
        }
    }

    private fun checkUserQuota(userId: Long, fileSizeBytes: Long) {
        val fileCount = fileMetadataRepository.countUserFiles(userId)
        if (fileCount >= MAX_FILES_PER_USER) {
            throw QuotaExceededException("User has reached maximum number of files: $MAX_FILES_PER_USER")
        }

        val usedBytes = fileMetadataRepository.getUserStorageUsedBytes(userId) ?: 0L
        val totalBytes = usedBytes + fileSizeBytes
        val maxBytes = MAX_FILE_SIZE_MB * 1024L * 1024L

        if (totalBytes > maxBytes) {
            throw QuotaExceededException("User storage quota exceeded: ${totalBytes / (1024 * 1024)}MB / ${MAX_FILE_SIZE_MB}MB")
        }
    }

    private fun toFileMetadataResponse(fileMetadata: FileMetadata): FileMetadataResponse {
        return FileMetadataResponse(
            id = fileMetadata.id,
            userId = fileMetadata.userId,
            fileKey = fileMetadata.fileKey,
            fileName = fileMetadata.fileName,
            fileType = fileMetadata.fileType.name,
            contentType = fileMetadata.contentType,
            fileSizeBytes = fileMetadata.fileSizeBytes,
            fileSizeMB = BigDecimal(fileMetadata.fileSizeBytes).divide(BigDecimal(1024 * 1024)),
            status = fileMetadata.status.name,
            isPublic = fileMetadata.isPublic,
            downloadCount = fileMetadata.downloadCount,
            checksumMd5 = fileMetadata.checksumMd5,
            createdAt = fileMetadata.createdAt,
            updatedAt = fileMetadata.updatedAt,
            expiresAt = fileMetadata.expiresAt,
            metadata = fileMetadata.metadata?.let { deserializeMetadata(it) }
        )
    }

    private fun generateDownloadUrl(fileKey: String): String {
        return "/api/files/download/$fileKey"
    }

    private fun serializeMetadata(metadata: Map<String, String>): String {
        // В реальности использовать ObjectMapper.writeValueAsString()
        return metadata.toString()
    }

    private fun deserializeMetadata(metadata: String): Map<String, String> {
        // В реальности использовать ObjectMapper.readValue()
        return emptyMap()
    }
}
