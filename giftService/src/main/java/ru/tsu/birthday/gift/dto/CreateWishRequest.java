package ru.tsu.birthday.gift.dto;

import jakarta.validation.constraints.*;

public record CreateWishRequest(
        @NotBlank @Size(max = 160) String title,
        @Size(max = 1000) String description,
        @Size(max = 500) @Pattern(regexp = "https?://[^\\s]+", message = "must be an http or https link") String link,
        @Min(1) @Max(10) int priority
) {}
