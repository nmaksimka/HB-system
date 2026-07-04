package ru.tsu.birthday.identity.service;

import java.time.LocalDate;
import java.time.Month;
import java.time.Year;
import java.time.temporal.ChronoUnit;

public final class Birthdays {
    private Birthdays() {}

    public static LocalDate next(LocalDate birthDate, LocalDate today) {
        var thisYear = inYear(birthDate, today.getYear());
        return thisYear.isBefore(today) ? inYear(birthDate, today.getYear() + 1) : thisYear;
    }

    public static int daysUntil(LocalDate birthDate, LocalDate today) {
        return (int) ChronoUnit.DAYS.between(today, next(birthDate, today));
    }

    private static LocalDate inYear(LocalDate birthDate, int year) {
        if (birthDate.getMonth() == Month.FEBRUARY && birthDate.getDayOfMonth() == 29 && !Year.isLeap(year)) {
            return LocalDate.of(year, Month.FEBRUARY, 28);
        }
        return birthDate.withYear(year);
    }
}
