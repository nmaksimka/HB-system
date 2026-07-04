package ru.tsu.birthday.fundraiser.event;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import ru.tsu.birthday.fundraiser.entity.FundraiserStatus;

public record FundraiserClosed(
        UUID eventId,
        UUID fundraiserId,
        FundraiserStatus status,
        BigDecimal collectedAmount,
        Instant closedAt
) {
    public static final String TOPIC = "fundraiser.closed";
}
