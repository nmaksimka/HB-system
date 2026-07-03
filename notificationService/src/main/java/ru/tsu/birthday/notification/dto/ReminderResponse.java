package ru.tsu.birthday.notification.dto;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record ReminderResponse(
        UUID id,
        UUID birthdayUserId,
        String birthdayUserName,
        LocalDate birthdayDate,
        Instant createdAt,
        Instant readAt
) {}
