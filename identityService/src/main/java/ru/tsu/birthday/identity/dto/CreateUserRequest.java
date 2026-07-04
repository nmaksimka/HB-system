package ru.tsu.birthday.identity.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

public record CreateUserRequest(
        @NotBlank @Size(max = 120) String name,
        @NotNull @Past LocalDate birthDate
) {}
