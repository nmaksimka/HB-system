package ru.tsu.birthday.chat.application;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.tsu.birthday.chat.domain.ChatMessage;
import java.time.Instant;
import java.util.*;

@Service
@Transactional
public class ChatService {
    private final ChatMessageRepository repository;
    public ChatService(ChatMessageRepository repository) { this.repository = repository; }

    public ChatMessage send(UUID subjectUserId, UUID authorId, String text) {
        if (subjectUserId.equals(authorId)) {
            throw new IllegalArgumentException("The birthday person cannot participate in their secret gift chat");
        }
        return repository.save(new ChatMessage(UUID.randomUUID(), subjectUserId, authorId, text, Instant.now()));
    }

    @Transactional(readOnly = true)
    public List<ChatMessage> history(UUID subjectUserId, UUID viewerId) {
        if (subjectUserId.equals(viewerId)) {
            throw new IllegalArgumentException("The birthday person cannot read their secret gift chat");
        }
        return repository.findTop100BySubjectUserIdOrderByCreatedAtAsc(subjectUserId);
    }
}
