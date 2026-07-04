package ru.tsu.birthday.notification.repository;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import ru.tsu.birthday.notification.entity.Reminder;

public interface ReminderRepository extends JpaRepository<Reminder, UUID> {
    List<Reminder> findByRecipientIdOrderByBirthdayDateAscThresholdDaysAsc(UUID recipientId);

    List<Reminder> findByRecipientIdAndReadAtIsNull(UUID recipientId);

    @Modifying
    @Query(value = """
            INSERT INTO reminders (id, recipient_id, birthday_user_id, birthday_user_name, birthday_date,
                                   threshold_days, created_at)
            VALUES (:id, :recipientId, :birthdayUserId, :birthdayUserName, :birthdayDate, :thresholdDays, :createdAt)
            ON CONFLICT (recipient_id, birthday_user_id, birthday_date, threshold_days) DO NOTHING
            """, nativeQuery = true)
    int insertIfAbsent(UUID id, UUID recipientId, UUID birthdayUserId, String birthdayUserName,
                       LocalDate birthdayDate, int thresholdDays, Instant createdAt);
}
