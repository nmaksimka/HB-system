package ru.tsu.birthday.fundraiser.event;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record PaymentSucceeded(
        UUID eventId,
        UUID paymentId,
        UUID fundraiserId,
        UUID payerId,
        BigDecimal amount,
        Instant paidAt
) {
    public static final String TOPIC = "payment.succeeded";
}
