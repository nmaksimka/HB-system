package ru.tsu.birthday.gift.dto;

import java.util.UUID;

public record WishResponse(
        UUID id,
        UUID userId,
        String title,
        String description,
        String link,
        int priority,
        boolean reserved,
        boolean reservedByMe
) {}
