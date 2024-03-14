package ru.test.gge.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.querydsl.QuerydslPredicateExecutor;
import ru.test.gge.model.File;

public interface FileRepository extends JpaRepository<File, Long>, QuerydslPredicateExecutor<File> {
    boolean existsByFileNameIgnoreCaseAndContentTypeIgnoreCase(String fileName, String contentType);
}