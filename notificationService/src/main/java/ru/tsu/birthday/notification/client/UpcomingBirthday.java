package ru.tsu.birthday.notification.client;

import java.time.LocalDate;
import java.util.UUID;

public record UpcomingBirthday(UUID id, String name, LocalDate birthDate, LocalDate nextBirthday, int daysLeft) {}
