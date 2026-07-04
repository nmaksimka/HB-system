package ru.tsu.birthday.fundraiser.repository;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import ru.tsu.birthday.fundraiser.entity.OutboxEvent;

public interface OutboxEventRepository extends JpaRepository<OutboxEvent, UUID> {
    List<OutboxEvent> findTop50BySentAtIsNullOrderByCreatedAtAsc();
}
