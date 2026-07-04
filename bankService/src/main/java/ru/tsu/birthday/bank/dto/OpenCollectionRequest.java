package ru.tsu.birthday.bank.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.util.UUID;

public record OpenCollectionRequest(
        @NotNull UUID fundraiserId,
        @NotNull UUID beneficiaryId,
        @NotNull @DecimalMin("1.00") BigDecimal targetAmount
) {}
