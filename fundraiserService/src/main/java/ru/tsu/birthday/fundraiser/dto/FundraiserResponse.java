package ru.tsu.birthday.fundraiser.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import ru.tsu.birthday.fundraiser.entity.FundraiserStatus;

public record FundraiserResponse(
        UUID id,
        UUID ownerId,
        UUID beneficiaryId,
        UUID wishId,
        String title,
        BigDecimal targetAmount,
        BigDecimal collectedAmount,
        FundraiserStatus status,
        Instant createdAt,
        Instant closedAt,
        List<ContributionResponse> contributions
) {}
