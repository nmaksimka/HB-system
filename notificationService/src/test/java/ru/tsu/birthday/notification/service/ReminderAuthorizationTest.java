package ru.tsu.birthday.notification.service;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import ru.tsu.birthday.notification.client.IdentityClient;
import ru.tsu.birthday.notification.entity.Reminder;
import ru.tsu.birthday.notification.repository.ReminderRepository;

import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ReminderAuthorizationTest {
    @Test
    void rejectsReadingAnotherUsersReminder() {
        var repository = mock(ReminderRepository.class);
        var service = new ReminderService(repository, mock(IdentityClient.class), Clock.systemUTC());
        var ownerId = UUID.randomUUID();
        var attackerId = UUID.randomUUID();
        var reminderId = UUID.randomUUID();
        var reminder = new Reminder(reminderId, ownerId, UUID.randomUUID(), "Anna",
                LocalDate.now(), 7, Instant.now());
        when(repository.findById(reminderId)).thenReturn(Optional.of(reminder));

        assertThatIllegalArgumentException()
                .isThrownBy(() -> service.markRead(attackerId, reminderId));
    }
}
