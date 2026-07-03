package ru.tsu.birthday.notification.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.tsu.birthday.notification.client.IdentityClient;
import ru.tsu.birthday.notification.entity.Reminder;
import ru.tsu.birthday.notification.repository.ReminderRepository;
import java.time.*;
import java.util.*;

@Service
@Transactional
public class ReminderService {
    private final ReminderRepository repository;
    private final IdentityClient identity;
    private final Clock clock;

    public ReminderService(ReminderRepository repository, IdentityClient identity, Clock clock) {
        this.repository = repository;
        this.identity = identity;
        this.clock = clock;
    }

    public List<Reminder> refresh(UUID recipientId, int days) {
        var today = LocalDate.now(clock);
        for (var user : identity.upcoming(recipientId, days)) {
            var next = nextBirthday(user.birthDate(), today);
            if (!repository.existsByRecipientIdAndBirthdayUserIdAndBirthdayDate(recipientId, user.id(), next)) {
                repository.save(new Reminder(
                        UUID.randomUUID(), recipientId, user.id(), user.name(), next, Instant.now(clock)));
            }
        }
        return repository.findByRecipientIdOrderByBirthdayDateAsc(recipientId);
    }

    public Reminder markRead(UUID recipientId, UUID reminderId) {
        var reminder = repository.findById(reminderId)
                .orElseThrow(() -> new NoSuchElementException("Reminder not found: " + reminderId));
        if (!reminder.getRecipientId().equals(recipientId)) {
            throw new IllegalArgumentException("Reminder belongs to another user");
        }
        reminder.markRead(Instant.now(clock));
        return reminder;
    }

    static LocalDate nextBirthday(LocalDate birthDate, LocalDate today) {
        var day = dateInYear(birthDate, today.getYear());
        return day.isBefore(today) ? dateInYear(birthDate, today.getYear() + 1) : day;
    }

    private static LocalDate dateInYear(LocalDate birthDate, int year) {
        if (birthDate.getMonth() == Month.FEBRUARY && birthDate.getDayOfMonth() == 29 && !Year.isLeap(year)) {
            return LocalDate.of(year, 2, 28);
        }
        return birthDate.withYear(year);
    }
}
