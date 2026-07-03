package ru.tsu.birthday.identity.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.UUID;

public record CreateGroupRequest(
        @NotBlank @Size(max = 160) String name,
        @NotNull UUID ownerId
) {}
