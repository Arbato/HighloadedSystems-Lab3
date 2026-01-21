package ru.itmo.fileservice.model.dto.request

import jakarta.validation.constraints.*

data class UploadFileRequest(
    @field:NotBlank(message = "File name is required")
    @field:Size(min = 1, max = 255, message = "File name must be between 1 and 255 characters")
    val fileName: String,

    @field:NotNull(message = "File content is required")
    val fileContent: ByteArray,

    @field:NotBlank(message = "Content type is required")
    val contentType: String,

    @field:Positive(message = "File size must be positive")
    val fileSizeBytes: Long,

    @field:Email(message = "Must be valid email") 
    val userEmail: String? = null,

    val isPublic: Boolean = false,

    val expiresInMinutes: Int? = null,

    val metadata: Map<String, String>? = null
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is UploadFileRequest) return false
        if (fileName != other.fileName) return false
        if (!fileContent.contentEquals(other.fileContent)) return false
        if (contentType != other.contentType) return false
        return fileSizeBytes == other.fileSizeBytes
    }

    override fun hashCode(): Int {
        var result = fileName.hashCode()
        result = 31 * result + fileContent.contentHashCode()
        result = 31 * result + contentType.hashCode()
        result = 31 * result + fileSizeBytes.hashCode()
        return result
    }
}
