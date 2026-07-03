package ru.tsu.birthday.identity.dto;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record FriendCardResponse(
        UUID id,
        String name,
        LocalDate birthDate,
        List<String> groups,
        List<WishResponse> wishes,
        String chatUrl
) {}
