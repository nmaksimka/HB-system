package ru.tsu.birthday.fundraiser.repository;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import ru.tsu.birthday.fundraiser.entity.ProcessedEvent;

public interface ProcessedEventRepository extends JpaRepository<ProcessedEvent, UUID> {}
