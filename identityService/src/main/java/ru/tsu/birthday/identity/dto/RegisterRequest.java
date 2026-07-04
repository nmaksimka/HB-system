package ru.tsu.birthday.identity.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

public record RegisterRequest(
        @NotBlank @Size(min = 3, max = 60) String username,
        @NotBlank @Size(min = 4, max = 120) String password,
        @Past LocalDate birthDate
) {}
