package ru.tsu.birthday.notification.application;

import org.junit.jupiter.api.Test;
import java.time.LocalDate;
import static org.assertj.core.api.Assertions.assertThat;

class ReminderServiceTest {
    @Test
    void choosesNextYearWhenBirthdayHasPassed() {
        var result = ReminderService.nextBirthday(
                LocalDate.of(2000, 1, 10),
                LocalDate.of(2026, 2, 1));
        assertThat(result).isEqualTo(LocalDate.of(2027, 1, 10));
    }

    @Test
    void normalizesLeapDay() {
        var result = ReminderService.nextBirthday(
                LocalDate.of(2000, 2, 29),
                LocalDate.of(2026, 2, 1));
        assertThat(result).isEqualTo(LocalDate.of(2026, 2, 28));
    }
}
