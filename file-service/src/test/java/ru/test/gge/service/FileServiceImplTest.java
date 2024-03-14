package ru.test.gge.service;

import com.querydsl.core.types.Predicate;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.web.multipart.MultipartFile;
import ru.test.gge.dto.FileDto;
import ru.test.gge.error.exception.FileValidationException;
import ru.test.gge.model.File;
import ru.test.gge.repository.FileRepository;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@ActiveProfiles("test")
class FileServiceImplTest {

    @Mock
    private FileRepository fileRepository;

    @InjectMocks
    private FileServiceImpl fileService;

    @Test
    @DisplayName("Выброс исключения при попытке сохранить пустой файл")
    void saveFile_WithEmptyFile_ThrowsException() {
        MultipartFile multipartFile = mock(MultipartFile.class);
        when(multipartFile.isEmpty()).thenReturn(true);

        assertThrows(FileValidationException.class, () -> fileService.saveFile(multipartFile));
    }

    @Test
    @DisplayName("Выброс исключения при попытке сохранить файл без имени")
    void saveFile_WithNoName_ThrowsException() {
        MultipartFile multipartFile = mock(MultipartFile.class);
        when(multipartFile.isEmpty()).thenReturn(false);
        when(multipartFile.getOriginalFilename()).thenReturn("");

        assertThrows(FileValidationException.class, () -> fileService.saveFile(multipartFile));
    }

    @Test
    @DisplayName("Выброс исключения при попытке сохранить файл с недопустимыми символами в имени")
    void saveFile_WithInvalidCharsInName_ThrowsException() {
        MultipartFile multipartFile = new MockMultipartFile("file", "invalid<>.txt", "text/plain", "content".getBytes());

        assertThrows(FileValidationException.class, () -> fileService.saveFile(multipartFile));
    }

    @Test
    @DisplayName("Выброс исключения при попытке сохранить файл с слишком длинным именем")
    void saveFile_WithTooLongName_ThrowsException() {
        String longName = "a".repeat(256) + ".txt";
        MultipartFile multipartFile = new MockMultipartFile("file", longName, "text/plain", "content".getBytes());

        assertThrows(FileValidationException.class, () -> fileService.saveFile(multipartFile));
    }

    @Test
    @DisplayName("Выброс исключения при попытке сохранить файл, который уже существует")
    void saveFile_WithExistingFile_ThrowsException() {
        MultipartFile multipartFile = new MockMultipartFile("file", "test.txt", "text/plain", "content".getBytes());
        when(fileRepository.existsByFileNameIgnoreCaseAndContentTypeIgnoreCase(anyString(), anyString())).thenReturn(true);

        assertThrows(FileValidationException.class, () -> fileService.saveFile(multipartFile));
    }

    @Test
    @DisplayName("Выброс исключения при неверных датах")
    void listFiles_WithInvalidDates_ThrowsException() {
        assertThrows(FileValidationException.class, () ->
                fileService.listFiles(0, 10, "id", "ASC", null, false, null, null, null, "2020-01-01 00:00:00", "2019-01-01 00:00:00"));
    }

    @Test
    @DisplayName("Фильтрация по имени файла с точным соответствием")
    void listFiles_WithExactNameMatch() {
        File file = new File();
        file.setFileName("exactName");
        file.setDateOfCreated(LocalDateTime.now());
        List<File> files = List.of(file);

        when(fileRepository.findAll(any(Predicate.class), any(Pageable.class))).thenReturn(new PageImpl<>(files));

        List<FileDto> result = fileService.listFiles(0, 10, "id", "ASC", "exactName", true, null, null, null, null, null);

        assertFalse(result.isEmpty());
        assertEquals("exactName", result.get(0).getFileName());
        verify(fileRepository).findAll(any(Predicate.class), any(PageRequest.class));
    }

    @Test
    @DisplayName("Фильтрация по имени файла без точного соответствия")
    void listFiles_WithPartialNameMatch() {
        File file = new File();
        file.setFileName("partialNameTest");
        file.setDateOfCreated(LocalDateTime.now());
        List<File> files = List.of(file);

        when(fileRepository.findAll(any(Predicate.class), any(Pageable.class))).thenReturn(new PageImpl<>(files));

        List<FileDto> result = fileService.listFiles(0, 10, "id", "ASC", "partial", false, null, null, null, null, null);

        assertFalse(result.isEmpty());
        assertTrue(result.get(0).getFileName().contains("partial"));
        verify(fileRepository).findAll(any(Predicate.class), any(Pageable.class));
    }
}