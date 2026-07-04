package ru.tsu.birthday.fundraiser.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record ContributionResponse(UUID payerId, BigDecimal amount, Instant paidAt) {}
