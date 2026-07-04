package ru.tsu.birthday.bank.event;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record FundraiserClosed(
        UUID eventId,
        UUID fundraiserId,
        String status,
        BigDecimal collectedAmount,
        Instant closedAt
) {
    public static final String TOPIC = "fundraiser.closed";
}
