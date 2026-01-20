package ru.itmo.fileservice.application.service

import org.slf4j.LoggerFactory
import org.springframework.core.io.buffer.DataBuffer
import org.springframework.http.codec.multipart.FilePart
import org.springframework.stereotype.Service
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import ru.itmo.fileservice.adapter.out.storage.MinioStorageAdapter
import ru.itmo.fileservice.domain.model.FileMetadata
import ru.itmo.fileservice.domain.model.dto.response.FileListResponse
import ru.itmo.fileservice.domain.model.dto.response.FileResponse
import ru.itmo.fileservice.domain.model.dto.response.UploadResponse
import java.time.Instant
import java.util.UUID

@Service
class FileService(
    private val storageAdapter: MinioStorageAdapter
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    fun uploadFile(filePart: FilePart, bucket: String): Mono<UploadResponse> {
        val fileId = UUID.randomUUID().toString()
        val originalFilename = filePart.filename()
        val extension = originalFilename.substringAfterLast(".", "")
        val storedFilename = if (extension.isNotEmpty()) "$fileId.$extension" else fileId

        logger.info("Uploading file: $originalFilename as $storedFilename to bucket: $bucket")

        return storageAdapter.uploadFile(filePart, bucket, storedFilename)
            .map { size ->
                val contentType = filePart.headers().contentType?.toString() ?: "application/octet-stream"
                UploadResponse(
                    id = fileId,
                    filename = storedFilename,
                    size = size,
                    contentType = contentType,
                    downloadUrl = "/api/files/$bucket/$storedFilename"
                )
            }
            .doOnSuccess { logger.info("File uploaded successfully: ${it.filename}") }
            .doOnError { logger.error("Failed to upload file: $originalFilename", it) }
    }

    fun downloadFile(bucket: String, filename: String): Flux<DataBuffer> {
        logger.info("Downloading file: $filename from bucket: $bucket")
        return storageAdapter.downloadFile(bucket, filename)
    }

    fun getFileMetadata(bucket: String, filename: String): Mono<FileResponse> {
        logger.info("Getting metadata for file: $filename from bucket: $bucket")
        return storageAdapter.getFileMetadata(bucket, filename)
            .map { metadata ->
                FileResponse(
                    id = filename.substringBeforeLast("."),
                    filename = metadata.filename,
                    originalFilename = metadata.originalFilename,
                    contentType = metadata.contentType,
                    size = metadata.size,
                    uploadedAt = metadata.uploadedAt,
                    downloadUrl = "/api/files/$bucket/$filename"
                )
            }
    }

    fun listFiles(bucket: String): Mono<FileListResponse> {
        logger.info("Listing files in bucket: $bucket")
        return storageAdapter.listFiles(bucket)
            .map { metadata ->
                FileResponse(
                    id = metadata.filename.substringBeforeLast("."),
                    filename = metadata.filename,
                    originalFilename = metadata.originalFilename,
                    contentType = metadata.contentType,
                    size = metadata.size,
                    uploadedAt = metadata.uploadedAt,
                    downloadUrl = "/api/files/$bucket/${metadata.filename}"
                )
            }
            .collectList()
            .map { files ->
                FileListResponse(files = files, total = files.size)
            }
    }

    fun deleteFile(bucket: String, filename: String): Mono<Void> {
        logger.info("Deleting file: $filename from bucket: $bucket")
        return storageAdapter.deleteFile(bucket, filename)
            .doOnSuccess { logger.info("File deleted successfully: $filename") }
            .doOnError { logger.error("Failed to delete file: $filename", it) }
    }

    fun createBucket(bucket: String): Mono<Boolean> {
        logger.info("Creating bucket: $bucket")
        return storageAdapter.createBucket(bucket)
    }
}
