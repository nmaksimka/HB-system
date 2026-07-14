package ru.tsu.birthday.bank.event;

import java.util.UUID;

public record PaymentRejected(UUID eventId, UUID paymentId, UUID fundraiserId) {
    public static final String TOPIC = "payment.rejected";
}
