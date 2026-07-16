package ru.tsu.birthday.notification.service;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.support.TransactionTemplate;
import ru.tsu.birthday.notification.client.IdentityClient;
import ru.tsu.birthday.notification.entity.Reminder;
import ru.tsu.birthday.notification.repository.ReminderRepository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ReminderAuthorizationTest {
    private final ReminderRepository repository = mock(ReminderRepository.class);
    private final Clock clock = Clock.fixed(Instant.parse("2026-01-01T10:00:00Z"), ZoneOffset.UTC);
    private final ReminderService service = new ReminderService(repository, mock(IdentityClient.class),
            mock(TransactionTemplate.class), clock);

    @Test
    void rejectsReadingAnotherUsersReminder() {
        var ownerId = UUID.randomUUID();
        var attackerId = UUID.randomUUID();
        var reminderId = UUID.randomUUID();
        var reminder = new Reminder(reminderId, ownerId, UUID.randomUUID(), "Anna",
                LocalDate.of(2026, 1, 8), 7, Instant.now());
        when(repository.findById(reminderId)).thenReturn(Optional.of(reminder));

        assertThatThrownBy(() -> service.markRead(attackerId, reminderId))
                .isInstanceOf(NoSuchElementException.class);
        assertThat(reminder.getReadAt()).isNull();
    }

    @Test
    void rejectsReadingMissingReminder() {
        var reminderId = UUID.randomUUID();
        when(repository.findById(reminderId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.markRead(UUID.randomUUID(), reminderId))
                .isInstanceOf(NoSuchElementException.class);
    }

    @Test
    void marksOwnReminderAsRead() {
        var ownerId = UUID.randomUUID();
        var reminderId = UUID.randomUUID();
        var reminder = new Reminder(reminderId, ownerId, UUID.randomUUID(), "Anna",
                LocalDate.of(2026, 1, 8), 7, Instant.now());
        when(repository.findById(reminderId)).thenReturn(Optional.of(reminder));

        var result = service.markRead(ownerId, reminderId);

        assertThat(result.getReadAt()).isEqualTo(clock.instant());
    }
}
