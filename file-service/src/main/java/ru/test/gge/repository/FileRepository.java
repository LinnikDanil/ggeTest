package ru.test.gge.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.test.gge.model.File;

public interface FileRepository extends JpaRepository<File, Long> {
}
