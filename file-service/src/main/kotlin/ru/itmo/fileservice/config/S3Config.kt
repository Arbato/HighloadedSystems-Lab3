package ru.itmo.fileservice.config

import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.s3.S3Client
import software.amazon.awssdk.services.s3.S3ClientBuilder
import java.net.URI

@Configuration
@ConditionalOnProperty(name = arrayOf("file.storage.type"), havingValue = "s3")
class S3Config(
    @Value("\${file.storage.s3.region:us-east-1}") private val region: String,
    @Value("\${file.storage.s3.access-key}") private val accessKey: String,
    @Value("\${file.storage.s3.secret-key}") private val secretKey: String,
    @Value("\${file.storage.s3.endpoint:}") private val endpoint: String
) {

    @Bean
    fun s3Client(): S3Client {
        val credentials = AwsBasicCredentials.create(accessKey, secretKey)
        val credentialsProvider = StaticCredentialsProvider.create(credentials)

        val builder: S3ClientBuilder = S3Client.builder()
            .region(Region.of(region))
            .credentialsProvider(credentialsProvider)

        // Для MinIO или других S3-совместимых сервисов
        if (endpoint.isNotEmpty()) {
            builder.endpointOverride(URI(endpoint))
        }

        return builder.build()
    }
}
