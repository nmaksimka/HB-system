package ru.tsu.birthday.chat.repository;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import ru.tsu.birthday.chat.entity.ChatMessage;

public interface ChatMessageRepository extends JpaRepository<ChatMessage, UUID> {
    List<ChatMessage> findTop100BySubjectUserIdOrderByCreatedAtDesc(UUID subjectUserId);
}
