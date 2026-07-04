package ru.tsu.birthday.bank.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record PaymentResponse(UUID id, UUID fundraiserId, UUID payerId, BigDecimal amount, Instant paidAt) {}
