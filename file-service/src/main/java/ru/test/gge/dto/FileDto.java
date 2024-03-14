package ru.test.gge.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class FileDto {
    private Long id;
    private String fileName;
    private String contentType;
    private Long size;
    private String dateOfCreated;
}
