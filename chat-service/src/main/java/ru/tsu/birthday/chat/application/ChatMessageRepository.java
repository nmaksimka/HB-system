package ru.tsu.birthday.chat.application;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.tsu.birthday.chat.domain.ChatMessage;
import java.util.*;

public interface ChatMessageRepository extends JpaRepository<ChatMessage, UUID> {
    List<ChatMessage> findTop100BySubjectUserIdOrderByCreatedAtAsc(UUID subjectUserId);
}
