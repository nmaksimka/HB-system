package ru.tsu.birthday.chat.service;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import ru.tsu.birthday.chat.client.IdentityClient;
import ru.tsu.birthday.chat.entity.ChatMessage;
import ru.tsu.birthday.chat.repository.ChatMessageRepository;

@Service
public class ChatService {
    private final ChatMessageRepository repository;
    private final IdentityClient identityClient;
    private final Clock clock;

    public ChatService(ChatMessageRepository repository, IdentityClient identityClient, Clock clock) {
        this.repository = repository;
        this.identityClient = identityClient;
        this.clock = clock;
    }

    public ChatMessage send(UUID subjectUserId, UUID authorId, String text) {
        requireAccess(subjectUserId, authorId);
        return repository.save(new ChatMessage(UUID.randomUUID(), subjectUserId, authorId, text.trim(),
                Instant.now(clock)));
    }

    public List<ChatMessage> history(UUID subjectUserId, UUID viewerId) {
        requireAccess(subjectUserId, viewerId);
        return repository.findTop100BySubjectUserIdOrderByCreatedAtDesc(subjectUserId).reversed();
    }

    public void requireAccess(UUID subjectUserId, UUID viewerId) {
        if (subjectUserId.equals(viewerId)) {
            throw new AccessDeniedException("The birthday person cannot see their secret gift chat");
        }
        if (!identityClient.canDiscuss(subjectUserId, viewerId)) {
            throw new AccessDeniedException("Add this person as a friend or join a common group to see the chat");
        }
    }
}
