package ru.itmo.fileservice.model.enums

enum class FileStatus {
    PENDING,           // В ожидании обработки
    PROCESSING,        // Идёт обработка (сканирование вирусов, конвертирование)
    ACTIVE,            // Активный файл
    ARCHIVED,          // Архивирован
    DELETED,           // Удалён (soft delete)
    SCAN_FAILED,       // Ошибка при сканировании вирусов
    UPLOAD_FAILED      // Ошибка загрузки
}
