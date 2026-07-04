package ru.tsu.birthday.fundraiser.dto;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.util.UUID;

public record CreateFundraiserRequest(
        @NotNull UUID wishId,
        @NotBlank @Size(max = 160) String title,
        @NotNull @DecimalMin("1.00") @DecimalMax("10000000.00") @Digits(integer = 10, fraction = 2)
        BigDecimal targetAmount
) {}
