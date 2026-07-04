package ru.tsu.birthday.notification.dto;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record ReminderResponse(
        UUID id,
        UUID birthdayUserId,
        String birthdayUserName,
        LocalDate birthdayDate,
        Integer thresholdDays,
        Instant createdAt,
        Instant readAt
) {}
