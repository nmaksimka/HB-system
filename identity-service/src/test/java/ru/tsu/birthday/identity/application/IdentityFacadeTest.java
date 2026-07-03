package ru.tsu.birthday.identity.application;

import org.junit.jupiter.api.Test;
import java.time.LocalDate;
import static org.assertj.core.api.Assertions.assertThat;

class IdentityFacadeTest {
    @Test
    void calculatesBirthdayAcrossYearBoundary() {
        var today = LocalDate.of(2026, 12, 30);
        assertThat(IdentityFacade.daysUntil(LocalDate.of(2000, 1, 2), today)).isEqualTo(3);
    }

    @Test
    void treatsTodayAsUpcomingBirthday() {
        var today = LocalDate.of(2026, 7, 3);
        assertThat(IdentityFacade.daysUntil(LocalDate.of(2000, 7, 3), today)).isZero();
    }

    @Test
    void handlesLeapDayInNonLeapYear() {
        var today = LocalDate.of(2026, 2, 27);
        assertThat(IdentityFacade.daysUntil(LocalDate.of(2000, 2, 29), today)).isEqualTo(1);
    }
}
