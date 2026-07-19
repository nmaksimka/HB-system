package ru.tsu.birthday.identity.service;

import java.time.LocalDate;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class BirthdaysTest {
    @Test
    void calculatesBirthdayAcrossYearBoundary() {
        var today = LocalDate.of(2026, 12, 30);

        assertThat(Birthdays.daysUntil(LocalDate.of(2000, 1, 2), today)).isEqualTo(3);
        assertThat(Birthdays.next(LocalDate.of(2000, 1, 2), today)).isEqualTo(LocalDate.of(2027, 1, 2));
    }

    @Test
    void treatsTodayAsUpcomingBirthday() {
        var today = LocalDate.of(2026, 7, 3);

        assertThat(Birthdays.daysUntil(LocalDate.of(2000, 7, 3), today)).isZero();
        assertThat(Birthdays.next(LocalDate.of(2000, 7, 3), today)).isEqualTo(today);
    }

    @Test
    void choosesNextYearWhenBirthdayHasPassed() {
        var today = LocalDate.of(2026, 2, 1);

        assertThat(Birthdays.next(LocalDate.of(2000, 1, 10), today)).isEqualTo(LocalDate.of(2027, 1, 10));
    }

    @Test
    void movesLeapDayToFebruary28InNonLeapYear() {
        var today = LocalDate.of(2026, 2, 27);

        assertThat(Birthdays.next(LocalDate.of(2000, 2, 29), today)).isEqualTo(LocalDate.of(2026, 2, 28));
        assertThat(Birthdays.daysUntil(LocalDate.of(2000, 2, 29), today)).isEqualTo(1);
    }

    @Test
    void celebratesLeapDayOnFebruary28Itself() {
        var today = LocalDate.of(2026, 2, 28);

        assertThat(Birthdays.daysUntil(LocalDate.of(2000, 2, 29), today)).isZero();
    }

    @Test
    void keepsLeapDayInLeapYear() {
        var today = LocalDate.of(2028, 2, 28);

        assertThat(Birthdays.next(LocalDate.of(2000, 2, 29), today)).isEqualTo(LocalDate.of(2028, 2, 29));
        assertThat(Birthdays.daysUntil(LocalDate.of(2000, 2, 29), today)).isEqualTo(1);
    }

    @Test
    void treatsLeapDayAsTodayInLeapYear() {
        var today = LocalDate.of(2028, 2, 29);

        assertThat(Birthdays.daysUntil(LocalDate.of(2000, 2, 29), today)).isZero();
    }

    @Test
    void waitsForNextLeapDayAfterMarch1() {
        var today = LocalDate.of(2027, 3, 1);

        assertThat(Birthdays.next(LocalDate.of(2000, 2, 29), today)).isEqualTo(LocalDate.of(2028, 2, 29));
        assertThat(Birthdays.daysUntil(LocalDate.of(2000, 2, 29), today)).isEqualTo(365);
    }

    @Test
    void birthdayOnMarch1IsOneDayAfterFebruary28InNonLeapYear() {
        var today = LocalDate.of(2027, 2, 28);

        assertThat(Birthdays.daysUntil(LocalDate.of(2000, 3, 1), today)).isEqualTo(1);
    }
}
