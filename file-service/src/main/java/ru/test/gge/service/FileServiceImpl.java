package ru.test.gge.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import ru.test.gge.dto.FileDto;
import ru.test.gge.dto.FileMapper;
import ru.test.gge.dto.FileShortDto;
import ru.test.gge.error.exception.FileNotFoundException;
import ru.test.gge.error.exception.FileValidationException;
import ru.test.gge.model.File;
import ru.test.gge.model.FileChunk;
import ru.test.gge.repository.FileRepository;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class FileServiceImpl implements FileService {

    private final FileRepository fileRepository;

    @Value("${file.chunk.size}")
    private int chunkSize;

    @Override
    public FileShortDto saveFile(MultipartFile multipartFile) throws IOException {
        //Проверка, что файл не пустой
        if (multipartFile.isEmpty()) {
            log.error("Попытка загрузки пустого файла");
            throw new FileValidationException("Пустой файл не может быть загружен");
        }

        //Проверка, что имя файла существует и что оно не пустое
        if (multipartFile.getOriginalFilename() == null || multipartFile.getOriginalFilename().isBlank()) {
            throw new FileValidationException("Имя файла не должно быть пустым.");
        }

        log.info("Начало загрузки файла: {}", multipartFile.getOriginalFilename());
        File file = new File();
        file.setFileName(multipartFile.getOriginalFilename());
        file.setContentType(multipartFile.getContentType());
        file.setSize(multipartFile.getSize());

        List<FileChunk> chunks = new ArrayList<>();
        byte[] bytes = multipartFile.getBytes();

        for (int start = 0; start < bytes.length; start += chunkSize) {
            // Определяем конец текущего чанка, не превышая общую длину
            int end = Math.min(bytes.length, start + chunkSize);
            // Создаём массив нужного размера
            byte[] chunkData = new byte[end - start];

            // Копируем текущую часть данных файла в текущий чанк.
            // bytes - исходный массив, start - начальный индекс в исходном массиве,
            // chunkData - целевой массив, 0 - начальный индекс в целевом массиве,
            // end - start - количество копируемых байтов.
            System.arraycopy(bytes, start, chunkData, 0, end - start);

            FileChunk chunk = new FileChunk();
            chunk.setData(chunkData);
            chunk.setFile(file);
            chunks.add(chunk);
        }

        file.setChunks(chunks);

        File savedFile = fileRepository.save(file);
        log.info("Файл успешно загружен: {}", file.getFileName());

        return new FileShortDto(savedFile.getId());
    }

    @Override
    public List<FileDto> listFiles(int from, int size, String sort) {
        log.info("Получение списка файлов: страница {}, размер {}, сортировка {}", from, size, sort);
        Pageable pageable = PageRequest.of(from, size, Sort.Direction.fromString(sort), "id");

        return fileRepository.findAll(pageable).stream()
                .map(FileMapper::toFileDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public byte[] downloadFile(Long fileId) {
        log.info("Скачивание файла с ID: {}", fileId);

        File file = getFileById(fileId);
        List<FileChunk> chunks = file.getChunks();

        chunks.sort(Comparator.comparingLong(FileChunk::getId));
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream(); //поток вывода

        for (FileChunk chunk : chunks) {
            try {
                outputStream.write(chunk.getData());
            } catch (IOException e) {
                log.error("Ошибка при чтении чанков файла", e);
                throw new RuntimeException("Ошибка при чтении чанков файла", e);
            }
        }

        return outputStream.toByteArray();
    }

    @Override
    public FileDto downloadFileMetadata(Long fileId) {
        log.info("Получение метаданных файла с ID: {}", fileId);

        return FileMapper.toFileDto(getFileById(fileId));
    }

    @Override
    public void deleteFile(Long fileId) {
        log.info("Начало удаления файла с ID: {}", fileId);
        File file = getFileById(fileId);
        fileRepository.delete(file);
        log.info("Файл с ID: {} успешно удален", fileId);
    }

    private File getFileById(long fileId) {
        return fileRepository.findById(fileId).orElseThrow(
                () -> new FileNotFoundException("Файл с ID = " + fileId + " не найден"));
    }
}
