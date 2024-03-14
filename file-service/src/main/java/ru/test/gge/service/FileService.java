package ru.test.gge.service;

import org.springframework.web.multipart.MultipartFile;
import ru.test.gge.dto.FileDto;
import ru.test.gge.dto.FileShortDto;

import java.io.IOException;
import java.util.List;

public interface FileService {
    FileShortDto saveFile(MultipartFile file) throws IOException;

    byte[] downloadFile(Long fileId);

    void deleteFile(Long fileId);

    FileDto downloadFileMetadata(Long fileId);

    List<FileDto> listFiles(int from, int size, String sort, String sortDirection, String name, boolean exactMatch,
                            List<String> contentTypes, Long sizeMin, Long sizeMax, String dateMin, String dateMax);
}
