plugins {
    kotlin("jvm") version "1.9.20"
    kotlin("plugin.spring") version "1.9.20"
    kotlin("plugin.jpa") version "1.9.20"
    id("org.springframework.boot") version "3.2.1"
    id("io.spring.dependency-management") version "1.1.4"
    id("org.flywaydb.flyway") version "10.20.1"
    jacoco
}


group = "ru.itmo"
version = "1.0.0"
description = "File Service - Spring Boot + JPA"


java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(17)
    }
}


repositories {
    mavenCentral()
}


dependencyManagement {
    imports {
        mavenBom("org.springframework.cloud:spring-cloud-dependencies:2023.0.0")
    }
}


dependencies {
    // Spring Boot Web
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("org.springframework.boot:spring-boot-starter-validation")
    implementation("org.springframework.boot:spring-boot-starter-actuator")
    implementation("org.springframework.boot:spring-boot-starter-cache")
    implementation("org.springframework.boot:spring-boot-starter-logging")


    // Spring Cloud
    implementation("org.springframework.cloud:spring-cloud-starter-netflix-eureka-client")
    implementation("org.springframework.cloud:spring-cloud-starter-config")
    implementation("org.springframework.cloud:spring-cloud-starter-bootstrap")
    implementation("org.springframework.cloud:spring-cloud-starter-openfeign")
    implementation("org.springframework.cloud:spring-cloud-starter-loadbalancer")


    // Resilience4j
    implementation("io.github.resilience4j:resilience4j-spring-boot3:2.1.0")
    implementation("io.github.resilience4j:resilience4j-circuitbreaker:2.1.0")
    implementation("io.github.resilience4j:resilience4j-timelimiter:2.1.0")


    // Database
    runtimeOnly("org.postgresql:postgresql")
    
    // ✅ Flyway для миграций (с версиями!)
    implementation("org.flywaydb:flyway-core:10.20.1")
    implementation("org.flywaydb:flyway-database-postgresql:10.20.1")
    
    runtimeOnly("com.h2database:h2")


    // AWS S3
    implementation("software.amazon.awssdk:s3:2.20.26")


    // Apache Commons
    implementation("commons-io:commons-io:2.11.0")


    // Kotlin
    implementation("org.jetbrains.kotlin:kotlin-reflect")
    implementation("org.jetbrains.kotlin:kotlin-stdlib")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin")

    // ✅ Logging - КРИТИЧЕСКИ ВАЖНО для mu и KotlinLogging
    implementation("io.github.microutils:kotlin-logging-jvm:3.0.5")


    // OpenAPI / Swagger
    implementation("org.springdoc:springdoc-openapi-starter-webmvc-ui:2.0.2")


    // Dev tools
    developmentOnly("org.springframework.boot:spring-boot-devtools")


    // Testing
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.testcontainers:testcontainers:1.19.3")
    testImplementation("org.testcontainers:junit-jupiter:1.19.3")
    testImplementation("org.testcontainers:postgresql:1.19.3")
    testImplementation("io.mockk:mockk:1.13.5")
    testImplementation("org.mockito.kotlin:mockito-kotlin:5.1.0")
    testImplementation("org.mockito:mockito-core:5.2.0")
}


kotlin {
    compilerOptions {
        freeCompilerArgs.addAll("-Xjsr305=strict")
    }
}


allOpen {
    annotation("jakarta.persistence.Entity")
    annotation("jakarta.persistence.MappedSuperclass")
}


// ✅ Конфигурация Flyway
flyway {
    baselineOnMigrate = true
    baselineVersion = "0"
    baselineDescription = "Initial baseline"
    validateOnMigrate = true
    outOfOrder = false
    cleanDisabled = true
    locations = arrayOf("classpath:db/migration")
    table = "flyway_schema_history"
    sqlMigrationPrefix = "V"
    sqlMigrationSeparator = "__"
    sqlMigrationSuffixes = arrayOf(".sql")
}


tasks.withType<Test> {
    useJUnitPlatform()
    finalizedBy(tasks.jacocoTestReport)
}


tasks.jacocoTestReport {
    dependsOn(tasks.test)
    reports {
        xml.required.set(true)
        html.required.set(true)
    }
}
