package ru.itmo.fileservice.service

import mu.KotlinLogging
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import software.amazon.awssdk.core.sync.RequestBody
import software.amazon.awssdk.services.s3.S3Client
import software.amazon.awssdk.services.s3.model.*
import ru.itmo.fileservice.exception.StorageException
import java.io.File
import java.nio.file.Files
import java.nio.file.Paths

private val logger = KotlinLogging.logger {}

@Service
class FileStorageService(
    @Value("\${file.storage.type:local}") private val storageType: String,
    @Value("\${file.storage.local.path:./uploads}") private val localStoragePath: String,
    @Value("\${file.storage.s3.bucket-name:file-service}") private val s3BucketName: String,
    @Value("\${file.storage.s3.region:us-east-1}") private val s3Region: String,
    private val s3Client: S3Client?
) {

    companion object {
        private const val BUFFER_SIZE = 8192
    }

    fun saveFile(fileKey: String, fileName: String, content: ByteArray, contentType: String): String {
        return when (storageType.lowercase()) {
            "s3" -> saveToS3(fileKey, fileName, content, contentType)
            else -> saveToLocal(fileKey, content)
        }
    }

    fun getFile(storagePath: String): ByteArray {
        return when (storageType.lowercase()) {
            "s3" -> getFromS3(storagePath)
            else -> getFromLocal(storagePath)
        }
    }

    fun deleteFile(storagePath: String) {
        when (storageType.lowercase()) {
            "s3" -> deleteFromS3(storagePath)
            else -> deleteFromLocal(storagePath)
        }
    }

    // ==================== Local Storage ====================

    private fun saveToLocal(fileKey: String, content: ByteArray): String {
        try {
            val storageDir = Paths.get(localStoragePath).toFile()
            if (!storageDir.exists()) {
                storageDir.mkdirs()
            }

            val filePath = Paths.get(localStoragePath, fileKey)
            Files.write(filePath, content)

            logger.info { "File saved locally: $filePath" }
            return filePath.toString()

        } catch (e: Exception) {
            logger.error(e) { "Error saving file to local storage: $fileKey" }
            throw StorageException("Failed to save file locally: ${e.message}", e)
        }
    }

    private fun getFromLocal(storagePath: String): ByteArray {
        try {
            return Files.readAllBytes(Paths.get(storagePath))
        } catch (e: Exception) {
            logger.error(e) { "Error reading file from local storage: $storagePath" }
            throw StorageException("Failed to read file locally: ${e.message}", e)
        }
    }

    private fun deleteFromLocal(storagePath: String) {
        try {
            val file = File(storagePath)
            if (file.exists()) {
                file.delete()
                logger.info { "File deleted locally: $storagePath" }
            }
        } catch (e: Exception) {
            logger.error(e) { "Error deleting file from local storage: $storagePath" }
            throw StorageException("Failed to delete file locally: ${e.message}", e)
        }
    }

    // ==================== AWS S3 ====================

    private fun saveToS3(fileKey: String, fileName: String, content: ByteArray, contentType: String): String {
        if (s3Client == null) {
            throw StorageException("S3 client is not configured")
        }

        try {
            val putObjectRequest = PutObjectRequest.builder()
                .bucket(s3BucketName)
                .key(fileKey)
                .contentType(contentType)
                .metadata(mapOf(
                    "original-filename" to fileName,
                    "uploaded-at" to System.currentTimeMillis().toString()
                ))
                .build()

            s3Client.putObject(
                putObjectRequest,
                RequestBody.fromBytes(content)
            )

            logger.info { "File saved to S3: s3://$s3BucketName/$fileKey" }
            return "s3://$s3BucketName/$fileKey"

        } catch (e: Exception) {
            logger.error(e) { "Error saving file to S3: $fileKey" }
            throw StorageException("Failed to save file to S3: ${e.message}", e)
        }
    }

    private fun getFromS3(storagePath: String): ByteArray {
        if (s3Client == null) {
            throw StorageException("S3 client is not configured")
        }

        try {
            // Парсим путь: s3://bucket/key
            val key = storagePath.substringAfter("$s3BucketName/")

            val getObjectRequest = GetObjectRequest.builder()
                .bucket(s3BucketName)
                .key(key)
                .build()

            val response = s3Client.getObject(getObjectRequest)
            val content = response.readAllBytes()
            response.close()

            logger.info { "File retrieved from S3: $storagePath" }
            return content

        } catch (e: Exception) {
            logger.error(e) { "Error retrieving file from S3: $storagePath" }
            throw StorageException("Failed to retrieve file from S3: ${e.message}", e)
        }
    }

    private fun deleteFromS3(storagePath: String) {
        if (s3Client == null) {
            throw StorageException("S3 client is not configured")
        }

        try {
            val key = storagePath.substringAfter("$s3BucketName/")

            val deleteObjectRequest = DeleteObjectRequest.builder()
                .bucket(s3BucketName)
                .key(key)
                .build()

            s3Client.deleteObject(deleteObjectRequest)

            logger.info { "File deleted from S3: $storagePath" }

        } catch (e: Exception) {
            logger.error(e) { "Error deleting file from S3: $storagePath" }
            throw StorageException("Failed to delete file from S3: ${e.message}", e)
        }
    }

    // ==================== Batch Operations ====================

    fun deleteMultipleFiles(storagePaths: List<String>) {
        storagePaths.forEach { path ->
            try {
                deleteFile(path)
            } catch (e: Exception) {
                logger.error(e) { "Error deleting file: $path" }
            }
        }
    }
}
