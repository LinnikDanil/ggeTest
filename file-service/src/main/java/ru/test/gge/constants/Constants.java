package ru.test.gge.constants;

import java.time.format.DateTimeFormatter;

public class Constants {
    // Список символов, которые недопустимы в именах файлов Windows
    public static final String INVALID_CHARS = "\\/:*?\"<>|";

    public static final String TIMESTAMP_FORMAT = "yyyy-MM-dd HH:mm:ss";
    public static final DateTimeFormatter TIMESTAMP_FORMATTER = DateTimeFormatter.ofPattern(TIMESTAMP_FORMAT);
}
