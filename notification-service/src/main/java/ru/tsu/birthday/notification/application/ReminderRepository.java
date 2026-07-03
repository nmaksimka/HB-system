package ru.tsu.birthday.notification.application;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.tsu.birthday.notification.domain.Reminder;
import java.time.LocalDate;
import java.util.*;

public interface ReminderRepository extends JpaRepository<Reminder, UUID> {
    List<Reminder> findByRecipientIdOrderByBirthdayDateAsc(UUID recipientId);
    boolean existsByRecipientIdAndBirthdayUserIdAndBirthdayDate(UUID recipientId, UUID birthdayUserId,
                                                                 LocalDate birthdayDate);
}
