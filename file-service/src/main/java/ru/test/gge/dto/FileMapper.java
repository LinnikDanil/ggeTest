package ru.test.gge.dto;

import lombok.experimental.UtilityClass;
import ru.test.gge.model.File;

import static ru.test.gge.constants.Constants.TIMESTAMP_FORMATTER;

@UtilityClass
public class FileMapper {

    public static FileDto toFileDto(File file) {
        return FileDto.builder()
                .id(file.getId())
                .fileName(file.getFileName())
                .contentType(file.getContentType())
                .size(file.getSize())
                .dateOfCreated(file.getDateOfCreated().format(TIMESTAMP_FORMATTER))
                .build();
    }
}
