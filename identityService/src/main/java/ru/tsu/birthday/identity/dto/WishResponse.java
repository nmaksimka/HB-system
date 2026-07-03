package ru.tsu.birthday.identity.dto;

import java.util.UUID;

public record WishResponse(
        UUID id,
        String title,
        String description,
        String link,
        Integer priority
) {}
