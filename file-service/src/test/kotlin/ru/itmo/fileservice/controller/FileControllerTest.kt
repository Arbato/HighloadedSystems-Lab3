package ru.itmo.fileservice.controller

import com.fasterxml.jackson.databind.ObjectMapper
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.http.MediaType
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.TestPropertySource
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.*
import ru.itmo.fileservice.model.dto.request.UploadFileRequest
import ru.itmo.fileservice.model.dto.response.FileUploadResponse
import ru.itmo.fileservice.service.FileService
import java.time.LocalDateTime

@WebMvcTest(controllers = [FileController::class])
@ActiveProfiles("test")
@TestPropertySource(properties = [
    "spring.cloud.config.enabled=false",
    "eureka.client.enabled=false"
])
@org.springframework.context.annotation.Import(ru.itmo.fileservice.exception.GlobalExceptionHandler::class)
@DisplayName("FileController Tests")
class FileControllerTest {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @Autowired
    private lateinit var objectMapper: ObjectMapper

    @MockBean
    private lateinit var fileService: FileService

    @Test
    @DisplayName("POST /api/files/upload uploads file successfully")
    fun testUploadFile() {
        val userId = 1L
        val request = UploadFileRequest(
            fileName = "test.pdf",
            fileContent = byteArrayOf(1, 2, 3),
            contentType = "application/pdf",
            fileSizeBytes = 3L
        )
        val response = FileUploadResponse(
            fileId = 1L,
            fileKey = "uuid",
            fileName = "test.pdf",
            fileType = "DOCUMENT",
            fileSizeBytes = 3L,
            contentType = "application/pdf",
            status = "ACTIVE",
            downloadUrl = "/api/files/download/1-uuid",
            expiresAt = null,
            uploadedAt = LocalDateTime.now()
        )

        every { fileService.uploadFile(userId, request) }.returns(response)

        mockMvc.perform(post("/api/files/upload")
            .header("X-User-Id", userId)
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isCreated)
            .andExpect(jsonPath("$.fileId").value(1))
            .andExpect(jsonPath("$.fileName").value("test.pdf"))
    }

    @Test
    @DisplayName("GET /api/files/{fileId} returns file metadata")
    fun testGetFileMetadata() {
        val userId = 1L
        val fileId = 1L

        mockMvc.perform(get("/api/files/$fileId")
            .header("X-User-Id", userId))
            .andExpect(status().isOk)
    }

    @Test
    @DisplayName("GET /api/files returns user files")
    fun testGetUserFiles() {
        val userId = 1L

        mockMvc.perform(get("/api/files")
            .header("X-User-Id", userId)
            .param("page", "1")
            .param("pageSize", "20"))
            .andExpect(status().isOk)
    }

    @Test
    @DisplayName("DELETE /api/files/{fileId} deletes file")
    fun testDeleteFile() {
        val userId = 1L
        val fileId = 1L

        every { fileService.deleteFile(userId, any()) }.returns("File marked as deleted")

        mockMvc.perform(delete("/api/files/$fileId")
            .header("X-User-Id", userId))
            .andExpect(status().isOk)
    }
}
