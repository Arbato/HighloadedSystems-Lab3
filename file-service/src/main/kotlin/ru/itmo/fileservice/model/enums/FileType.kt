package ru.itmo.fileservice.model.enums

enum class FileType {
    IMAGE,             // Изображения (jpg, png, gif, webp)
    DOCUMENT,          // Документы (pdf, docx, xlsx, pptx)
    VIDEO,             // Видео (mp4, webm, avi)
    AUDIO,             // Аудио (mp3, wav, ogg)
    ARCHIVE,           // Архивы (zip, rar, 7z)
    CODE,              // Код (java, kt, py, js)
    OTHER              // Прочее
}
