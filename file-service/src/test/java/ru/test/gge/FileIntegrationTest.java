package ru.test.gge;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.*;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import ru.test.gge.dto.FileDto;
import ru.test.gge.dto.FileShortDto;

import java.io.IOException;
import java.util.Arrays;
import java.util.Objects;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
public class FileIntegrationTest {
    @Autowired
    private TestRestTemplate restTemplate;

    @LocalServerPort
    private int port;

    private String createURLWithPort(String uri) {
        // Создаем полный URL, добавляя порт к базовому пути
        return "http://localhost:" + port + uri;
    }

    @Test
    @DisplayName("Полный интеграционный тест работы с файлами")
    public void testCreateListDownloadAndDeleteFile() throws IOException {
        // Создание мок файла для тестирования
        MockMultipartFile mockFile = new MockMultipartFile("file", "testfile.txt",
                "text/plain", "Test content".getBytes());

        // Подготовка данных для отправки в запросе
        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        ByteArrayResource byteArrayResource = new ByteArrayResource(mockFile.getBytes()) {
            @Override
            public String getFilename() {
                // Переопределение метода для возвращения оригинального имени файла
                return mockFile.getOriginalFilename();
            }
        };
        body.add("file", byteArrayResource);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);

        // Создание HTTP запроса для загрузки файла
        HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(body, headers);
        ResponseEntity<FileShortDto> response = restTemplate.exchange(
                createURLWithPort("/files/upload"), HttpMethod.POST, requestEntity, FileShortDto.class);

        // Проверка статуса ответа и получение ID загруженного файла
        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertNotNull(response.getBody());
        Long uploadedFileId = response.getBody().getFileId();

        // Запрос на получение списка файлов и проверка, что наш файл в этом списке
        ResponseEntity<FileDto[]> listResponse = restTemplate.getForEntity(createURLWithPort("/files"), FileDto[].class);
        assertEquals(HttpStatus.OK, listResponse.getStatusCode());
        assertTrue(Arrays.stream(Objects.requireNonNull(listResponse.getBody())).anyMatch(f -> f.getId().equals(uploadedFileId)));

        // Запрос на скачивание файла и проверка его содержимого
        ResponseEntity<byte[]> downloadResponse = restTemplate.getForEntity(createURLWithPort("/files/" + uploadedFileId), byte[].class);
        assertEquals(HttpStatus.OK, downloadResponse.getStatusCode());
        assertNotNull(downloadResponse.getBody());
        assertEquals("Test content", new String(downloadResponse.getBody()));

        // Удаление файла
        restTemplate.delete(createURLWithPort("/files/" + uploadedFileId));

        // Попытка скачать удаленный файл должна вернуть статус NOT_FOUND
        ResponseEntity<byte[]> deletedFileResponse = restTemplate.getForEntity(createURLWithPort("/files/" + uploadedFileId), byte[].class);
        assertEquals(HttpStatus.NOT_FOUND, deletedFileResponse.getStatusCode());
    }
}
