package ru.tsu.birthday.notification.client;

import java.time.LocalDate;
import java.util.UUID;

public record UpcomingUserResponse(UUID id, String name, LocalDate birthDate) {}
