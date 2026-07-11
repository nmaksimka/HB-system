package ru.tsu.birthday.notification.service;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import ru.tsu.birthday.notification.client.IdentityClient;
import ru.tsu.birthday.notification.client.UpcomingBirthday;
import ru.tsu.birthday.notification.entity.Reminder;
import ru.tsu.birthday.notification.repository.ReminderRepository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ReminderServiceTest {
    private final ReminderRepository repository = mock(ReminderRepository.class);
    private final IdentityClient identityClient = mock(IdentityClient.class);
    private final TransactionTemplate transaction = new TransactionTemplate(mock(PlatformTransactionManager.class));
    private final Instant now = Instant.parse("2026-01-01T10:00:00Z");
    private final Clock clock = Clock.fixed(now, ZoneOffset.UTC);
    private final ReminderService service = new ReminderService(repository, identityClient, transaction, clock);

    @Test
    void choosesTodayMilestoneForZeroDays() {
        assertThat(ReminderService.milestoneFor(0)).hasValue(0);
    }

    @Test
    void choosesOneDayMilestoneForOneDay() {
        assertThat(ReminderService.milestoneFor(1)).hasValue(1);
    }

    @Test
    void roundsFiveDaysUpToWeekMilestone() {
        assertThat(ReminderService.milestoneFor(5)).hasValue(7);
    }

    @Test
    void keepsWeekMilestoneForSevenDays() {
        assertThat(ReminderService.milestoneFor(7)).hasValue(7);
    }

    @Test
    void roundsEightDaysUpToMonthMilestone() {
        assertThat(ReminderService.milestoneFor(8)).hasValue(30);
    }

    @Test
    void keepsMonthMilestoneForThirtyDays() {
        assertThat(ReminderService.milestoneFor(30)).hasValue(30);
    }

    @Test
    void hasNoMilestoneBeyondMonth() {
        assertThat(ReminderService.milestoneFor(31)).isEmpty();
    }

    @Test
    void insertsRemindersWithMatchingMilestones() {
        var recipientId = UUID.randomUUID();
        var today = upcoming("Today", 0);
        var soon = upcoming("Soon", 5);
        var later = upcoming("Later", 8);
        when(identityClient.upcoming(recipientId, 30)).thenReturn(List.of(today, soon, later));
        when(repository.findByRecipientIdOrderByBirthdayDateAscThresholdDaysAsc(recipientId)).thenReturn(List.of());

        service.refresh(recipientId);

        verify(repository).insertIfAbsent(any(UUID.class), eq(recipientId), eq(today.id()), eq("Today"),
                eq(today.nextBirthday()), eq(0), eq(now));
        verify(repository).insertIfAbsent(any(UUID.class), eq(recipientId), eq(soon.id()), eq("Soon"),
                eq(soon.nextBirthday()), eq(7), eq(now));
        verify(repository).insertIfAbsent(any(UUID.class), eq(recipientId), eq(later.id()), eq("Later"),
                eq(later.nextBirthday()), eq(30), eq(now));
    }

    @Test
    void skipsBirthdaysBeyondLastMilestone() {
        var recipientId = UUID.randomUUID();
        when(identityClient.upcoming(recipientId, 30)).thenReturn(List.of(upcoming("Far", 31)));
        when(repository.findByRecipientIdOrderByBirthdayDateAscThresholdDaysAsc(recipientId)).thenReturn(List.of());

        service.refresh(recipientId);

        verify(repository, never()).insertIfAbsent(any(), any(), any(), any(), any(), anyInt(), any());
    }

    @Test
    void returnsRemindersAfterRefresh() {
        var recipientId = UUID.randomUUID();
        var reminder = new Reminder(UUID.randomUUID(), recipientId, UUID.randomUUID(), "Anna",
                LocalDate.of(2026, 1, 8), 7, now);
        when(identityClient.upcoming(recipientId, 30)).thenReturn(List.of());
        when(repository.findByRecipientIdOrderByBirthdayDateAscThresholdDaysAsc(recipientId))
                .thenReturn(List.of(reminder));

        assertThat(service.refresh(recipientId)).containsExactly(reminder);
    }

    @Test
    void marksAllUnreadRemindersAsRead() {
        var recipientId = UUID.randomUUID();
        var first = new Reminder(UUID.randomUUID(), recipientId, UUID.randomUUID(), "Anna",
                LocalDate.of(2026, 1, 8), 7, now);
        var second = new Reminder(UUID.randomUUID(), recipientId, UUID.randomUUID(), "Bob",
                LocalDate.of(2026, 1, 9), 30, now);
        when(repository.findByRecipientIdAndReadAtIsNull(recipientId)).thenReturn(List.of(first, second));
        when(repository.findByRecipientIdOrderByBirthdayDateAscThresholdDaysAsc(recipientId))
                .thenReturn(List.of(first, second));

        var result = service.markAllRead(recipientId);

        assertThat(result).containsExactly(first, second);
        assertThat(first.getReadAt()).isEqualTo(now);
        assertThat(second.getReadAt()).isEqualTo(now);
    }

    @Test
    void markAllReadWithoutUnreadRemindersChangesNothing() {
        var recipientId = UUID.randomUUID();
        when(repository.findByRecipientIdAndReadAtIsNull(recipientId)).thenReturn(List.of());
        when(repository.findByRecipientIdOrderByBirthdayDateAscThresholdDaysAsc(recipientId)).thenReturn(List.of());

        assertThat(service.markAllRead(recipientId)).isEmpty();
    }

    private UpcomingBirthday upcoming(String name, int daysLeft) {
        var next = LocalDate.of(2026, 1, 1).plusDays(daysLeft);
        return new UpcomingBirthday(UUID.randomUUID(), name, LocalDate.of(1990, next.getMonth(), next.getDayOfMonth()),
                next, daysLeft);
    }
}
