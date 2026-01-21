package ru.itmo.fileservice.controller

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag
import mu.KotlinLogging
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.*
import ru.itmo.fileservice.model.dto.request.UploadFileRequest
import ru.itmo.fileservice.model.dto.request.DeleteFileRequest
import ru.itmo.fileservice.model.dto.request.UpdateFileMetadataRequest
import ru.itmo.fileservice.model.dto.response.*
import ru.itmo.fileservice.model.enums.FileType
import ru.itmo.fileservice.service.FileService
import jakarta.validation.Valid
import jakarta.validation.constraints.Positive

private val logger = KotlinLogging.logger {}

@RestController
@RequestMapping("/api/files")
@Validated
@Tag(name = "File Management", description = "File upload, download, and management operations")
class FileController(
    private val fileService: FileService
) {

    @PostMapping("/upload")
    @Operation(summary = "Upload a file")
    @SecurityRequirement(name = "bearer-jwt")
    fun uploadFile(
        @RequestHeader("X-User-Id") userId: Long,
        @Valid @RequestBody request: UploadFileRequest
    ): ResponseEntity<FileUploadResponse> {
        logger.info { "Upload file request from user $userId" }
        val response = fileService.uploadFile(userId, request)
        return ResponseEntity.status(HttpStatus.CREATED).body(response)
    }

    @GetMapping("/{fileId}")
    @Operation(summary = "Get file metadata")
    @SecurityRequirement(name = "bearer-jwt")
    fun getFileMetadata(
        @RequestHeader("X-User-Id") userId: Long,
        @PathVariable @Positive fileId: Long
    ): ResponseEntity<FileMetadataResponse> {
        logger.info { "Get metadata request for file $fileId from user $userId" }
        val response = fileService.getFileMetadata(userId, fileId)
        return ResponseEntity.ok(response)
    }

    @GetMapping
    @Operation(summary = "Get user's files (paginated)")
    @SecurityRequirement(name = "bearer-jwt")
    fun getUserFiles(
        @RequestHeader("X-User-Id") userId: Long,
        @RequestParam(defaultValue = "1") @Positive page: Int,
        @RequestParam(defaultValue = "20") @Positive pageSize: Int
    ): ResponseEntity<PaginatedFilesResponse> {
        logger.info { "Get files request from user $userId, page=$page, pageSize=$pageSize" }
        val response = fileService.getUserFiles(userId, page, pageSize)
        return ResponseEntity.ok(response)
    }

    @GetMapping("/type/{fileType}")
    @Operation(summary = "Get user's files by type")
    @SecurityRequirement(name = "bearer-jwt")
    fun getUserFilesByType(
        @RequestHeader("X-User-Id") userId: Long,
        @PathVariable fileType: FileType,
        @RequestParam(defaultValue = "1") @Positive page: Int,
        @RequestParam(defaultValue = "20") @Positive pageSize: Int
    ): ResponseEntity<PaginatedFilesResponse> {
        logger.info { "Get files by type request: type=$fileType, user=$userId" }
        val response = fileService.getUserFilesByType(userId, fileType, page, pageSize)
        return ResponseEntity.ok(response)
    }

    @DeleteMapping("/{fileId}")
    @Operation(summary = "Delete a file")
    @SecurityRequirement(name = "bearer-jwt")
    fun deleteFile(
        @RequestHeader("X-User-Id") userId: Long,
        @PathVariable @Positive fileId: Long,
        @RequestParam(defaultValue = "true") softDelete: Boolean
    ): ResponseEntity<Map<String, String>> {
        logger.info { "Delete file request: fileId=$fileId, user=$userId, softDelete=$softDelete" }
        val message = fileService.deleteFile(
            userId,
            DeleteFileRequest(fileId, softDelete)
        )
        return ResponseEntity.ok(mapOf("message" to message))
    }

    @PutMapping("/{fileId}")
    @Operation(summary = "Update file metadata")
    @SecurityRequirement(name = "bearer-jwt")
    fun updateFileMetadata(
        @RequestHeader("X-User-Id") userId: Long,
        @PathVariable @Positive fileId: Long,
        @Valid @RequestBody request: UpdateFileMetadataRequest
    ): ResponseEntity<FileMetadataResponse> {
        logger.info { "Update metadata request for file $fileId from user $userId" }
        val updateRequest = request.copy(fileId = fileId)
        val response = fileService.updateFileMetadata(userId, updateRequest)
        return ResponseEntity.ok(response)
    }

    @GetMapping("/statistics")
    @Operation(summary = "Get user's storage statistics")
    @SecurityRequirement(name = "bearer-jwt")
    fun getUserStatistics(
        @RequestHeader("X-User-Id") userId: Long
    ): ResponseEntity<FileStatisticsResponse> {
        logger.info { "Get statistics request from user $userId" }
        val response = fileService.getUserStatistics(userId)
        return ResponseEntity.ok(response)
    }

    @ExceptionHandler(Exception::class)
    fun handleException(e: Exception): ResponseEntity<Map<String, String>> {
        logger.error(e) { "Unexpected error in FileController" }
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
            .body(mapOf("error" to (e.message ?: "Unknown error")))
    }
}
