package ru.itmo.fileservice.controller

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import mu.KotlinLogging
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.*
import ru.itmo.fileservice.service.FileService
import jakarta.validation.constraints.NotBlank

private val logger = KotlinLogging.logger {}

@RestController
@RequestMapping("/api/files/download")
@Validated
@Tag(name = "File Download", description = "Download files operations")
class FileDownloadController(
    private val fileService: FileService
) {

    @GetMapping("/{fileKey}")
    @Operation(summary = "Download file by key")
    fun downloadFileByKey(
        @RequestHeader("X-User-Id") userId: Long,
        @PathVariable @NotBlank fileKey: String
    ): ResponseEntity<ByteArray> {
        logger.info { "Download request for file $fileKey from user $userId" }
        
        // Парсим fileKey если он содержит ID: format "1-uuid"
        val fileId = fileKey.substringBefore("-").toLongOrNull() ?: 
            throw IllegalArgumentException("Invalid file key format")

        val fileDownload = fileService.downloadFile(userId, fileId)

        return ResponseEntity.ok()
            .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"${fileDownload.fileName}\"")
            .header(HttpHeaders.CONTENT_LENGTH, fileDownload.fileSizeBytes.toString())
            .header(HttpHeaders.CONTENT_TYPE, fileDownload.contentType)
            .header("X-MD5-Checksum", fileDownload.checksumMd5 ?: "")
            .body(fileDownload.content)
    }

    @GetMapping("/{fileId}/inline")
    @Operation(summary = "View file inline (preview)")
    fun viewFileInline(
        @RequestHeader("X-User-Id") userId: Long,
        @PathVariable fileId: Long
    ): ResponseEntity<ByteArray> {
        logger.info { "View inline request for file $fileId from user $userId" }

        val fileDownload = fileService.downloadFile(userId, fileId)

        val mediaType = when {
            fileDownload.contentType.startsWith("image/") -> MediaType.parseMediaType(fileDownload.contentType)
            fileDownload.contentType == "application/pdf" -> MediaType.APPLICATION_PDF
            else -> MediaType.APPLICATION_OCTET_STREAM
        }

        return ResponseEntity.ok()
            .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"${fileDownload.fileName}\"")
            .header(HttpHeaders.CONTENT_LENGTH, fileDownload.fileSizeBytes.toString())
            .contentType(mediaType)
            .body(fileDownload.content)
    }

    @RequestMapping(method = [RequestMethod.HEAD], value = ["/{fileId}"])
    @Operation(summary = "Get file metadata without downloading content")
    fun getFileHeader(
        @RequestHeader("X-User-Id") userId: Long,
        @PathVariable fileId: Long
    ): ResponseEntity<Void> {
        logger.info { "HEAD request for file $fileId from user $userId" }

        val metadata = fileService.getFileMetadata(userId, fileId)

        return ResponseEntity.ok()
            .header(HttpHeaders.CONTENT_LENGTH, metadata.fileSizeBytes.toString())
            .header(HttpHeaders.CONTENT_TYPE, metadata.contentType)
            .header("X-MD5-Checksum", metadata.checksumMd5 ?: "")
            .build()
    }
}
