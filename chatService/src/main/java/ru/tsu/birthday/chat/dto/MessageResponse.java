package ru.tsu.birthday.chat.dto;

import java.time.Instant;
import java.util.UUID;

public record MessageResponse(
        UUID id,
        UUID subjectUserId,
        UUID authorId,
        String text,
        Instant createdAt
) {}
