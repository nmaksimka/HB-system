package ru.tsu.birthday.gift.dto;

import java.util.UUID;

public record WishSummary(UUID id, UUID userId, String title) {}
