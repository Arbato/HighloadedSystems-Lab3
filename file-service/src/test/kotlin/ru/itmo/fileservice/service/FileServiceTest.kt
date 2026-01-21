package ru.itmo.fileservice.service

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import ru.itmo.fileservice.exception.FileNotFoundException
import ru.itmo.fileservice.exception.FileValidationException
import ru.itmo.fileservice.exception.QuotaExceededException
import ru.itmo.fileservice.model.dto.request.UploadFileRequest
import ru.itmo.fileservice.model.entity.FileMetadata
import ru.itmo.fileservice.model.enums.FileStatus
import ru.itmo.fileservice.model.enums.FileType
import ru.itmo.fileservice.repository.FileMetadataRepository
import java.math.BigDecimal
import java.util.*

@DisplayName("FileService Tests")
class FileServiceTest {

    private lateinit var fileService: FileService
    private val fileMetadataRepository: FileMetadataRepository = mockk()
    private val fileStorageService: FileStorageService = mockk()
    private val fileValidationService: FileValidationService = mockk()

    @BeforeEach
    fun setUp() {
        fileService = FileService(
            fileMetadataRepository,
            fileStorageService,
            fileValidationService
        )
    }

    @Test
    @DisplayName("Should upload file successfully")
    fun testUploadFileSuccess() {
        val userId = 1L
        val request = UploadFileRequest(
            fileName = "test.pdf",
            fileContent = byteArrayOf(1, 2, 3),
            contentType = "application/pdf",
            fileSizeBytes = 3L
        )

        every { fileValidationService.validateUserId(userId) }.returns(Unit)
        every { fileValidationService.validateUploadRequest(request, any()) }.returns(Unit)
        every { fileMetadataRepository.countUserFiles(userId) }.returns(5L)
        every { fileMetadataRepository.getUserStorageUsedBytes(userId) }.returns(100L)
        every { fileStorageService.saveFile(any(), any(), any(), any()) }.returns("/uploads/uuid")
        every { fileValidationService.calculateMd5(request.fileContent) }.returns("abc123")
        every { fileMetadataRepository.save(any()) }.returnsArgument(0)

        val result = fileService.uploadFile(userId, request)

        assert(result.fileId > 0)
        assert(result.fileName == "test.pdf")
        verify { fileMetadataRepository.save(any()) }
    }

    @Test
    @DisplayName("Should throw exception when user quota exceeded")
    fun testUploadFileQuotaExceeded() {
        val userId = 1L
        val request = UploadFileRequest(
            fileName = "large.iso",
            fileContent = byteArrayOf(1),
            contentType = "application/octet-stream",
            fileSizeBytes = 600_000_000L  // 600 MB, превышает лимит 500 MB
        )

        every { fileValidationService.validateUserId(userId) }.returns(Unit)
        every { fileValidationService.validateUploadRequest(request, any()) }.throws(
            QuotaExceededException("File size exceeds limit")
        )

        assertThrows<QuotaExceededException> {
            fileService.uploadFile(userId, request)
        }
    }

    @Test
    @DisplayName("Should download file successfully")
    fun testDownloadFileSuccess() {
        val userId = 1L
        val fileId = 1L
        val fileMetadata = FileMetadata(
            id = fileId,
            userId = userId,
            fileKey = "uuid",
            fileName = "test.pdf",
            fileType = FileType.DOCUMENT,
            contentType = "application/pdf",
            fileSizeBytes = 1024L,
            status = FileStatus.ACTIVE,
            storagePath = "/uploads/uuid",
            checksumMd5 = "abc123"
        )

        every { fileValidationService.validateUserId(userId) }.returns(Unit)
        every { fileValidationService.validateFileId(fileId) }.returns(Unit)
        every { fileMetadataRepository.findByIdAndUserId(fileId, userId) }.returns(Optional.of(fileMetadata))
        every { fileStorageService.getFile("/uploads/uuid") }.returns(byteArrayOf(1, 2, 3))
        every { fileMetadataRepository.save(any()) }.returnsArgument(0)

        val result = fileService.downloadFile(userId, fileId)

        assert(result.fileId == fileId)
        assert(result.fileName == "test.pdf")
        verify { fileMetadataRepository.findByIdAndUserId(fileId, userId) }
    }

    @Test
    @DisplayName("Should throw exception when file not found")
    fun testDownloadFileNotFound() {
        val userId = 1L
        val fileId = 999L

        every { fileValidationService.validateUserId(userId) }.returns(Unit)
        every { fileValidationService.validateFileId(fileId) }.returns(Unit)
        every { fileMetadataRepository.findByIdAndUserId(fileId, userId) }.returns(Optional.empty())

        assertThrows<FileNotFoundException> {
            fileService.downloadFile(userId, fileId)
        }
    }
}
