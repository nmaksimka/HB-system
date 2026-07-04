package ru.tsu.birthday.gift.dto;

import java.time.Instant;
import java.util.UUID;

public record WishResponse(
        UUID id,
        String title,
        String description,
        String link,
        Integer priority,
        UUID reservedBy,
        Instant reservedAt
) {}
