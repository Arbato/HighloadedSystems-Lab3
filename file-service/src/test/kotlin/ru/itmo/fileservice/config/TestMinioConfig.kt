package ru.itmo.fileservice.config

import io.minio.MinioClient
import org.mockito.Mockito
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Primary
import org.springframework.context.annotation.Profile

@Configuration
@Profile("test")
class TestMinioConfig {

    @Bean
    @Primary
    fun minioClient(): MinioClient {
        return Mockito.mock(MinioClient::class.java)
    }
}
