package ru.tsu.birthday.identity.dto;

import java.util.UUID;

public record GroupResponse(UUID id, String name, UUID ownerId) {}
