package ru.tsu.birthday.identity.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

public record RegisterRequest(
        @NotBlank @Size(min = 3, max = 60) @Pattern(regexp = "[a-zA-Z0-9._-]+") String username,
        @NotBlank @Size(min = 6, max = 120) String password,
        @NotBlank @Size(max = 120) String name,
        @NotNull @Past LocalDate birthDate
) {}
