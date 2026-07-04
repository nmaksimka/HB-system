package ru.tsu.birthday.notification.service;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.OptionalInt;
import java.util.UUID;
import java.util.stream.IntStream;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import ru.tsu.birthday.notification.client.IdentityClient;
import ru.tsu.birthday.notification.entity.Reminder;
import ru.tsu.birthday.notification.repository.ReminderRepository;

@Service
public class ReminderService {
    static final int[] MILESTONES = {0, 1, 7, 30};
    static final int HORIZON_DAYS = 30;

    private final ReminderRepository repository;
    private final IdentityClient identity;
    private final TransactionTemplate transaction;
    private final Clock clock;

    public ReminderService(ReminderRepository repository, IdentityClient identity, TransactionTemplate transaction,
                           Clock clock) {
        this.repository = repository;
        this.identity = identity;
        this.transaction = transaction;
        this.clock = clock;
    }

    public List<Reminder> refresh(UUID recipientId) {
        var upcoming = identity.upcoming(recipientId, HORIZON_DAYS);
        var now = Instant.now(clock);
        transaction.executeWithoutResult(status -> {
            for (var birthday : upcoming) {
                milestoneFor(birthday.daysLeft()).ifPresent(milestone -> repository.insertIfAbsent(
                        UUID.randomUUID(), recipientId, birthday.id(), birthday.name(), birthday.nextBirthday(),
                        milestone, now));
            }
        });
        return reminders(recipientId);
    }

    @Transactional(readOnly = true)
    public List<Reminder> reminders(UUID recipientId) {
        return repository.findByRecipientIdOrderByBirthdayDateAscThresholdDaysAsc(recipientId);
    }

    @Transactional
    public Reminder markRead(UUID recipientId, UUID reminderId) {
        var reminder = repository.findById(reminderId)
                .filter(r -> r.getRecipientId().equals(recipientId))
                .orElseThrow(() -> new NoSuchElementException("Reminder not found: " + reminderId));
        reminder.markRead(Instant.now(clock));
        return reminder;
    }

    @Transactional
    public List<Reminder> markAllRead(UUID recipientId) {
        var now = Instant.now(clock);
        repository.findByRecipientIdAndReadAtIsNull(recipientId).forEach(reminder -> reminder.markRead(now));
        return repository.findByRecipientIdOrderByBirthdayDateAscThresholdDaysAsc(recipientId);
    }

    static OptionalInt milestoneFor(int daysLeft) {
        return IntStream.of(MILESTONES).filter(milestone -> daysLeft <= milestone).findFirst();
    }
}
