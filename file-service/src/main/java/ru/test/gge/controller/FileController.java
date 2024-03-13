package ru.test.gge.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import ru.test.gge.dto.FileDto;
import ru.test.gge.dto.FileShortDto;
import ru.test.gge.service.FileService;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;

@RestController
@RequestMapping("/files")
@Slf4j
@RequiredArgsConstructor
@Validated
@Tag(name = "File Controller", description = "Контроллер для работы с файлами")
public class FileController {

    private final FileService fileService;

    @Operation(summary = "Загрузка файла", description = "Позволяет загрузить файл на сервер")
    @ApiResponse(responseCode = "201", description = "Файл успешно загружен",
            content = {@Content(mediaType = "application/json",
                    schema = @Schema(implementation = FileShortDto.class))})
    @PostMapping("/upload")
    @ResponseStatus(HttpStatus.CREATED)
    public FileShortDto uploadFile(@RequestParam("file") MultipartFile file) throws IOException {
        log.info("Загрузка файла: {}, размером = {} байт", file.getOriginalFilename(), file.getSize());
        return fileService.saveFile(file);
    }

    @Operation(summary = "Получение списка файлов", description = "Возвращает список файлов с пагинацией и сортировкой")
    @ApiResponse(responseCode = "200", description = "Список файлов получен",
            content = {@Content(mediaType = "application/json",
                    schema = @Schema(implementation = FileDto.class))})
    @GetMapping
    public List<FileDto> listFiles(@RequestParam(value = "from", defaultValue = "0") @PositiveOrZero int from,
                                   @RequestParam(value = "size", defaultValue = "10") @Positive int size,
                                   @RequestParam(value = "sort", defaultValue = "asc") String sort) {
        log.info("Список файлов - from: {}, size: {}, sort: {}", from, size, sort);
        return fileService.listFiles(from, size, sort);
    }

    @Operation(summary = "Скачивание файла", description = "Позволяет скачать файл по ID")
    @ApiResponse(responseCode = "200", description = "Файл успешно скачан",
            content = {@Content(mediaType = "application/octet-stream")})
    @GetMapping("/{id}")
    public ResponseEntity<byte[]> downloadFile(@PathVariable Long id) {
        log.info("Скачивание файла с ID: {}", id);

        FileDto file = fileService.downloadFileMetadata(id);
        byte[] data = fileService.downloadFile(id);

        // Кодирование имени файла для использования в HTTP заголовке.
        String encodedFileName = URLEncoder.encode(file.getFileName(), StandardCharsets.UTF_8)
                .replace("+", "%20");

        HttpHeaders header = new HttpHeaders();
        header.setContentType(MediaType.parseMediaType(file.getContentType()));
        header.setContentLength(file.getSize());
        // Соответствие RFC 5987
        header.set(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename*=UTF-8''" + encodedFileName);

        return new ResponseEntity<>(data, header, HttpStatus.OK);
    }

    @Operation(summary = "Удаление файла", description = "Позволяет удалить файл по ID")
    @ApiResponse(responseCode = "204", description = "Файл успешно удален")
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteFile(@PathVariable Long id) {
        log.info("Удаление файла с ID: {}", id);
        fileService.deleteFile(id);
    }
}
