package ru.tsu.birthday.chat.domain;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "chat_messages")
public class ChatMessage {
    @Id private UUID id;
    @Column(nullable = false) private UUID subjectUserId;
    @Column(nullable = false) private UUID authorId;
    @Column(nullable = false, length = 2000) private String text;
    @Column(nullable = false, updatable = false) private Instant createdAt;

    protected ChatMessage() {}
    public ChatMessage(UUID id, UUID subjectUserId, UUID authorId, String text, Instant createdAt) {
        this.id = id; this.subjectUserId = subjectUserId; this.authorId = authorId;
        this.text = text; this.createdAt = createdAt;
    }
    public UUID getId() { return id; }
    public UUID getSubjectUserId() { return subjectUserId; }
    public UUID getAuthorId() { return authorId; }
    public String getText() { return text; }
    public Instant getCreatedAt() { return createdAt; }
}
