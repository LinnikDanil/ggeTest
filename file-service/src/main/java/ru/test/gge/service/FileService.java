package ru.test.gge.service;

import org.springframework.web.multipart.MultipartFile;
import ru.test.gge.dto.FileDto;
import ru.test.gge.dto.FileShortDto;

import java.io.IOException;
import java.util.List;

public interface FileService {
    FileShortDto saveFile(MultipartFile file) throws IOException;

    List<FileDto> listFiles(int from, int size, String sort);

    byte[] downloadFile(Long fileId);

    void deleteFile(Long fileId);

    FileDto downloadFileMetadata(Long fileId);
}
