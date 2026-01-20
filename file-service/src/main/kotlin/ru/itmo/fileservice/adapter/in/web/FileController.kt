package ru.itmo.fileservice.adapter.`in`.web

import org.springframework.core.io.buffer.DataBuffer
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.http.codec.multipart.FilePart
import org.springframework.web.bind.annotation.*
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import ru.itmo.fileservice.application.service.FileService
import ru.itmo.fileservice.domain.model.dto.response.FileListResponse
import ru.itmo.fileservice.domain.model.dto.response.FileResponse
import ru.itmo.fileservice.domain.model.dto.response.UploadResponse

@RestController
@RequestMapping("/api/files")
class FileController(
    private val fileService: FileService
) {

    @PostMapping("/{bucket}", consumes = [MediaType.MULTIPART_FORM_DATA_VALUE])
    fun uploadFile(
        @PathVariable bucket: String,
        @RequestPart("file") filePart: Mono<FilePart>
    ): Mono<ResponseEntity<UploadResponse>> {
        return filePart
            .flatMap { file -> fileService.uploadFile(file, bucket) }
            .map { response -> ResponseEntity.ok(response) }
    }

    @GetMapping("/{bucket}/{filename}", produces = [MediaType.APPLICATION_OCTET_STREAM_VALUE])
    fun downloadFile(
        @PathVariable bucket: String,
        @PathVariable filename: String
    ): Mono<ResponseEntity<Flux<DataBuffer>>> {
        return fileService.getFileMetadata(bucket, filename)
            .map { metadata ->
                ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"${metadata.filename}\"")
                    .header(HttpHeaders.CONTENT_TYPE, metadata.contentType)
                    .header(HttpHeaders.CONTENT_LENGTH, metadata.size.toString())
                    .body(fileService.downloadFile(bucket, filename))
            }
    }

    @GetMapping("/{bucket}/{filename}/metadata")
    fun getFileMetadata(
        @PathVariable bucket: String,
        @PathVariable filename: String
    ): Mono<ResponseEntity<FileResponse>> {
        return fileService.getFileMetadata(bucket, filename)
            .map { ResponseEntity.ok(it) }
    }

    @GetMapping("/{bucket}")
    fun listFiles(
        @PathVariable bucket: String
    ): Mono<ResponseEntity<FileListResponse>> {
        return fileService.listFiles(bucket)
            .map { ResponseEntity.ok(it) }
    }

    @DeleteMapping("/{bucket}/{filename}")
    fun deleteFile(
        @PathVariable bucket: String,
        @PathVariable filename: String
    ): Mono<ResponseEntity<Void>> {
        return fileService.deleteFile(bucket, filename)
            .then(Mono.just(ResponseEntity.noContent().build()))
    }

    @PostMapping("/buckets/{bucket}")
    fun createBucket(
        @PathVariable bucket: String
    ): Mono<ResponseEntity<Map<String, Any>>> {
        return fileService.createBucket(bucket)
            .map { created ->
                ResponseEntity.ok(
                    mapOf(
                        "bucket" to bucket,
                        "created" to created,
                        "message" to if (created) "Bucket created successfully" else "Bucket already exists"
                    )
                )
            }
    }
}
