package ru.tsu.birthday.fundraiser.client;

import java.util.UUID;

public record WishSummary(UUID id, UUID userId, String title) {}
