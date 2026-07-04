package ru.tsu.birthday.bank.dto;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;

public record PayRequest(
        @NotNull @DecimalMin("1.00") @Digits(integer = 10, fraction = 2) BigDecimal amount,
        @NotBlank @Size(max = 64) String idempotencyKey
) {}
