package ru.test.gge.dto;

import lombok.experimental.UtilityClass;
import ru.test.gge.model.File;

@UtilityClass
public class FileMapper {
    public static FileDto toFileDto(File file) {
        return FileDto.builder()
                .id(file.getId())
                .fileName(file.getFileName())
                .contentType(file.getContentType())
                .size(file.getSize())
                .build();
    }
}
