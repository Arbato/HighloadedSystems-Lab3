#!/bin/bash

# Имя выходного файла (если хотите сохранить в файл, раскомментируйте строку ниже)
OUTPUT_FILE="all_code.txt"

# Функция для обработки файлов
process_files() {
    # Ищем файлы, исключая директорию .git
    find . -type f -not -path '*/.git/*' | while read -r file; do
        # 1. Игнорируем сам скрипт, чтобы он не читал сам себя
        if [[ "$file" == "./$(basename "$0")" ]]; then continue; fi
        
        # 2. Игнорируем бинарные файлы (через grep), чтобы не вывести мусор
        if grep -qI . "$file" 2> /dev/null; then
            echo "$file"
            cat "$file"
            # Добавляем перенос строки на случай, если в конце файла его нет
            echo -e "\n"
        fi
    done
}

# Запуск функции. Можно перенаправить вывод в файл: ./script.sh > output.txt
process_files

