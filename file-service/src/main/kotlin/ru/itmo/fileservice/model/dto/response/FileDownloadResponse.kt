package ru.itmo.fileservice.model.dto.response

import java.time.LocalDateTime
import java.math.BigDecimal

data class FileDownloadResponse(
    val fileId: Long,
    val fileName: String,
    val contentType: String,
    val fileSizeBytes: Long,
    val content: ByteArray,
    val checksumMd5: String?,
    val createdAt: LocalDateTime,
    val downloadedAt: LocalDateTime = LocalDateTime.now()
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is FileDownloadResponse) return false
        if (fileId != other.fileId) return false
        if (fileName != other.fileName) return false
        return content.contentEquals(other.content)
    }

    override fun hashCode(): Int {
        var result = fileId.hashCode()
        result = 31 * result + fileName.hashCode()
        result = 31 * result + content.contentHashCode()
        return result
    }
}