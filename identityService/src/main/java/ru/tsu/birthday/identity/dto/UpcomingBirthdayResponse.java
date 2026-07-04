package ru.tsu.birthday.identity.dto;

import java.time.LocalDate;
import java.util.UUID;

public record UpcomingBirthdayResponse(UUID id, String name, LocalDate birthDate, LocalDate nextBirthday,
                                       int daysLeft) {}
