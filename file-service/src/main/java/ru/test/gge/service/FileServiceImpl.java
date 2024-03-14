package ru.test.gge.service;

import com.querydsl.core.BooleanBuilder;
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
import ru.test.gge.model.QFile;
import ru.test.gge.repository.FileRepository;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static ru.test.gge.constants.Constants.INVALID_CHARS;
import static ru.test.gge.constants.Constants.TIMESTAMP_FORMATTER;

@Service
@RequiredArgsConstructor
@Slf4j
public class FileServiceImpl implements FileService {

    private final FileRepository fileRepository;

    @Value("${file.chunk.size}")
    private int chunkSize;

    @Override
    public FileShortDto saveFile(MultipartFile multipartFile) throws IOException {
        validateFile(multipartFile);

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
    public List<FileDto> listFiles(int from, int size, String sort, String sortDirection,
                                   String name, boolean exactMatch, List<String> contentTypes,
                                   Long sizeMin, Long sizeMax, String dateMin, String dateMax) {
        log.info("Получение списка файлов с параметрами - начать с : {} элемента, размер: {}, сортировка: {}, направление сортировки: {}, " +
                        "имя: {}, точное совпадение: {}, типы содержимого: {}, мин. размер: {}, макс. размер: {}, дата начала: {}, дата окончания: {}",
                from, size, sort, sortDirection, name, exactMatch, contentTypes, sizeMin, sizeMax, dateMin, dateMax);


        Sort sortOrder = Sort.by(Sort.Direction.fromString(sortDirection), sort);
        Pageable pageable = PageRequest.of(from / size, size, sortOrder);

        LocalDateTime startDate =
                dateMin != null ? LocalDateTime.parse(dateMin, TIMESTAMP_FORMATTER) : LocalDateTime.now().minusYears(100);
        LocalDateTime endDate =
                dateMax != null ? LocalDateTime.parse(dateMax, TIMESTAMP_FORMATTER) : LocalDateTime.now();

        if (startDate.isAfter(endDate)) {
            throw new FileValidationException("Начальная дата не может превышать окончательную");
        }

        // создание Predicate для Querydsl
        BooleanBuilder builder = new BooleanBuilder();
        Optional.ofNullable(name).ifPresent(n -> {
            if (exactMatch) {
                builder.and(QFile.file.fileName.equalsIgnoreCase(n));
            } else {
                builder.and(QFile.file.fileName.containsIgnoreCase(n));
            }
        });
        Optional.ofNullable(contentTypes).ifPresent(ct -> builder.and(QFile.file.contentType.in(ct)));
        Optional.ofNullable(sizeMin).ifPresent(min -> builder.and(QFile.file.size.goe(min)));
        Optional.ofNullable(sizeMax).ifPresent(max -> builder.and(QFile.file.size.loe(max)));
        builder.and(QFile.file.dateOfCreated.between(startDate, endDate));

        List<File> files = fileRepository.findAll(builder.getValue(), pageable).getContent();

        return files.stream()
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

    private void validateFile(MultipartFile file) {
        if (file.isEmpty()) {
            log.error("Попытка загрузки пустого файла");
            throw new FileValidationException("Пустой файл не может быть загружен");
        }

        String fileName = file.getOriginalFilename();
        if (fileName == null || fileName.isBlank()) {
            log.error("Попытка загрузки файла с пустым именем");
            throw new FileValidationException("Имя файла не должно быть пустым.");
        }

        if (fileName.length() > 255) {
            log.error("Попытка загрузки слишком длинного файла");
            throw new FileValidationException("Имя файла содержит слишком длинное.");
        }

        // Проверка каждого символа имени файла
        for (char c : fileName.toCharArray()) {
            if (INVALID_CHARS.indexOf(c) != -1) {
                log.error("Попытка загрузки файла с системными символами");
                throw new FileValidationException("Имя файла содержит недопустимые символы.");
            }
        }

        if (fileRepository.existsByFileNameIgnoreCaseAndContentTypeIgnoreCase(fileName, file.getContentType())) {
            log.error("Попытка загрузки файла с существующим именем");
            throw new FileValidationException("Файл с таким именем уже существует.");
        }
    }
}
