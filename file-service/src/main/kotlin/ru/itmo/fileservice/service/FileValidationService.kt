package ru.itmo.fileservice.service

import mu.KotlinLogging
import org.springframework.stereotype.Service
import ru.itmo.fileservice.exception.FileValidationException
import ru.itmo.fileservice.model.dto.request.UploadFileRequest
import java.security.MessageDigest

private val logger = KotlinLogging.logger {}

@Service
class FileValidationService {

    companion object {
        private val ALLOWED_EXTENSIONS = setOf(
            "jpg", "jpeg", "png", "gif", "webp", "bmp",  // Images
            "pdf", "docx", "doc", "xlsx", "xls", "pptx", "ppt", "txt", "rtf",  // Documents
            "mp4", "avi", "mov", "mkv", "webm", "flv",  // Videos
            "mp3", "wav", "ogg", "aac", "flac",  // Audio
            "zip", "rar", "7z", "tar", "gz",  // Archives
            "kt", "java", "py", "js", "ts", "go", "rs", "cpp", "c", "h",  // Code
            "csv", "json", "xml", "yaml", "yml"  // Data formats
        )

        private val BLOCKED_EXTENSIONS = setOf(
            "exe", "bat", "cmd", "sh", "com", "msi",  // Executables
            "dll", "sys", "drv",  // System files
            "vbs", "js", "jse", "vbe",  // Scripts
            "scr", "pif", "app"  // Others
        )

        private const val MIN_FILE_SIZE_BYTES = 1
        private const val ALLOWED_MIME_TYPES_PATTERN = "^(image|video|audio|application|text)/"
    }

    fun validateUserId(userId: Long) {
        if (userId <= 0) {
            throw FileValidationException("Invalid user ID: $userId")
        }
    }

    fun validateFileId(fileId: Long) {
        if (fileId <= 0) {
            throw FileValidationException("Invalid file ID: $fileId")
        }
    }

    fun validatePagination(page: Int, pageSize: Int) {
        if (page < 1) {
            throw FileValidationException("Page must be >= 1, got: $page")
        }
        if (pageSize < 1 || pageSize > 100) {
            throw FileValidationException("Page size must be between 1 and 100, got: $pageSize")
        }
    }

    fun validateUploadRequest(request: UploadFileRequest, maxFileSizeMB: Int) {
        // Проверить имя файла
        if (request.fileName.isBlank()) {
            throw FileValidationException("File name cannot be blank")
        }

        if (request.fileName.length > 255) {
            throw FileValidationException("File name exceeds 255 characters")
        }

        // Проверить расширение файла
        val fileExtension = request.fileName.substringAfterLast(".", "").lowercase()
        
        if (fileExtension in BLOCKED_EXTENSIONS) {
            throw FileValidationException("File type not allowed: .$fileExtension")
        }

        if (!fileExtension.isEmpty() && fileExtension !in ALLOWED_EXTENSIONS) {
            logger.warn { "Unusual file extension: .$fileExtension" }
        }

        // Проверить MIME type
        if (!request.contentType.matches(Regex(ALLOWED_MIME_TYPES_PATTERN))) {
            throw FileValidationException("Content type not allowed: ${request.contentType}")
        }

        // Проверить размер файла
        if (request.fileSizeBytes < MIN_FILE_SIZE_BYTES) {
            throw FileValidationException("File size is too small: ${request.fileSizeBytes} bytes")
        }

        val maxSizeBytes = maxFileSizeMB * 1024L * 1024L
        if (request.fileSizeBytes > maxSizeBytes) {
            throw FileValidationException("File size exceeds limit: ${request.fileSizeBytes} bytes > ${maxSizeBytes} bytes")
        }

        // Проверить содержимое байт-массива
        if (request.fileContent.isEmpty()) {
            throw FileValidationException("File content is empty")
        }

        if (request.fileContent.size != request.fileSizeBytes.toInt()) {
            throw FileValidationException("File size mismatch: declared=${request.fileSizeBytes}, actual=${request.fileContent.size}")
        }
    }

    fun calculateMd5(content: ByteArray): String {
        val digest = MessageDigest.getInstance("MD5")
        val hashBytes = digest.digest(content)
        return hashBytes.joinToString("") { "%02x".format(it) }
    }

    fun validateChecksum(content: ByteArray, expectedMd5: String): Boolean {
        val calculatedMd5 = calculateMd5(content)
        return calculatedMd5 == expectedMd5
    }
}
