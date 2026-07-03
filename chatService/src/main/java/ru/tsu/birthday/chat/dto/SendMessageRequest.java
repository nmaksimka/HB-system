package ru.tsu.birthday.chat.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.UUID;

public record SendMessageRequest(
        @NotNull UUID authorId,
        @NotBlank @Size(max = 2000) String text
) {}
