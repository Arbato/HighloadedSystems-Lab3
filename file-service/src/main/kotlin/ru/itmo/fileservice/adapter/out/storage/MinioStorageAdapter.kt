package ru.itmo.fileservice.adapter.out.storage

import io.minio.*
import io.minio.messages.Item
import org.slf4j.LoggerFactory
import org.springframework.core.io.buffer.DataBuffer
import org.springframework.core.io.buffer.DataBufferUtils
import org.springframework.core.io.buffer.DefaultDataBufferFactory
import org.springframework.http.codec.multipart.FilePart
import org.springframework.stereotype.Component
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import reactor.core.scheduler.Schedulers
import ru.itmo.fileservice.domain.model.FileMetadata
import java.io.InputStream
import java.io.PipedInputStream
import java.io.PipedOutputStream
import java.time.Instant

@Component
class MinioStorageAdapter(
    private val minioClient: MinioClient
) {
    private val logger = LoggerFactory.getLogger(javaClass)
    private val bufferFactory = DefaultDataBufferFactory()

    fun uploadFile(filePart: FilePart, bucket: String, filename: String): Mono<Long> {
        return Mono.fromCallable {
            ensureBucketExists(bucket)

            val pipedOutputStream = PipedOutputStream()
            val pipedInputStream = PipedInputStream(pipedOutputStream)

            val contentType = filePart.headers().contentType?.toString() ?: "application/octet-stream"

            // Write data in background
            DataBufferUtils.write(filePart.content(), pipedOutputStream)
                .subscribeOn(Schedulers.boundedElastic())
                .doFinally { pipedOutputStream.close() }
                .subscribe()

            // Upload to MinIO
            val result = minioClient.putObject(
                PutObjectArgs.builder()
                    .bucket(bucket)
                    .`object`(filename)
                    .stream(pipedInputStream, -1, 10485760) // 10MB part size
                    .contentType(contentType)
                    .build()
            )

            // Get object size
            val stat = minioClient.statObject(
                StatObjectArgs.builder()
                    .bucket(bucket)
                    .`object`(filename)
                    .build()
            )
            stat.size()
        }.subscribeOn(Schedulers.boundedElastic())
    }

    fun downloadFile(bucket: String, filename: String): Flux<DataBuffer> {
        return Mono.fromCallable {
            minioClient.getObject(
                GetObjectArgs.builder()
                    .bucket(bucket)
                    .`object`(filename)
                    .build()
            )
        }
            .subscribeOn(Schedulers.boundedElastic())
            .flatMapMany { inputStream ->
                DataBufferUtils.readInputStream(
                    { inputStream },
                    bufferFactory,
                    8192
                )
            }
    }

    fun getFileMetadata(bucket: String, filename: String): Mono<FileMetadata> {
        return Mono.fromCallable {
            val stat = minioClient.statObject(
                StatObjectArgs.builder()
                    .bucket(bucket)
                    .`object`(filename)
                    .build()
            )
            FileMetadata(
                id = filename.substringBeforeLast("."),
                filename = filename,
                originalFilename = filename,
                contentType = stat.contentType(),
                size = stat.size(),
                bucket = bucket,
                uploadedAt = stat.lastModified().toInstant()
            )
        }.subscribeOn(Schedulers.boundedElastic())
    }

    fun listFiles(bucket: String): Flux<FileMetadata> {
        return Mono.fromCallable {
            ensureBucketExists(bucket)
            minioClient.listObjects(
                ListObjectsArgs.builder()
                    .bucket(bucket)
                    .build()
            )
        }
            .subscribeOn(Schedulers.boundedElastic())
            .flatMapMany { results ->
                Flux.fromIterable(results)
                    .map { result -> result.get() }
                    .map { item ->
                        FileMetadata(
                            id = item.objectName().substringBeforeLast("."),
                            filename = item.objectName(),
                            originalFilename = item.objectName(),
                            contentType = "application/octet-stream",
                            size = item.size(),
                            bucket = bucket,
                            uploadedAt = item.lastModified()?.toInstant() ?: Instant.now()
                        )
                    }
            }
    }

    fun deleteFile(bucket: String, filename: String): Mono<Void> {
        return Mono.fromRunnable<Void> {
            minioClient.removeObject(
                RemoveObjectArgs.builder()
                    .bucket(bucket)
                    .`object`(filename)
                    .build()
            )
        }.subscribeOn(Schedulers.boundedElastic())
    }

    fun createBucket(bucket: String): Mono<Boolean> {
        return Mono.fromCallable {
            if (!minioClient.bucketExists(BucketExistsArgs.builder().bucket(bucket).build())) {
                minioClient.makeBucket(MakeBucketArgs.builder().bucket(bucket).build())
                logger.info("Bucket created: $bucket")
                true
            } else {
                logger.info("Bucket already exists: $bucket")
                false
            }
        }.subscribeOn(Schedulers.boundedElastic())
    }

    private fun ensureBucketExists(bucket: String) {
        if (!minioClient.bucketExists(BucketExistsArgs.builder().bucket(bucket).build())) {
            minioClient.makeBucket(MakeBucketArgs.builder().bucket(bucket).build())
            logger.info("Bucket created: $bucket")
        }
    }
}
