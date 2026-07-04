package ru.tsu.birthday.notification.entity;

import jakarta.persistence.*;
import java.time.*;
import java.util.UUID;

@Entity
@Table(name = "reminders",
       uniqueConstraints = @UniqueConstraint(
               columnNames = {"recipient_id", "birthday_user_id", "birthday_date", "threshold_days"}))
public class Reminder {
    @Id private UUID id;
    @Column(nullable = false) private UUID recipientId;
    @Column(nullable = false) private UUID birthdayUserId;
    @Column(nullable = false, length = 120) private String birthdayUserName;
    @Column(nullable = false) private LocalDate birthdayDate;
    @Column(nullable = false) private Integer thresholdDays;
    @Column(nullable = false) private Instant createdAt;
    private Instant readAt;

    protected Reminder() {}
    public Reminder(UUID id, UUID recipientId, UUID birthdayUserId, String birthdayUserName,
                    LocalDate birthdayDate, Integer thresholdDays, Instant createdAt) {
        this.id = id; this.recipientId = recipientId; this.birthdayUserId = birthdayUserId;
        this.birthdayUserName = birthdayUserName; this.birthdayDate = birthdayDate;
        this.thresholdDays = thresholdDays; this.createdAt = createdAt;
    }
    public void markRead(Instant timestamp) { readAt = timestamp; }
    public UUID getId() { return id; }
    public UUID getRecipientId() { return recipientId; }
    public UUID getBirthdayUserId() { return birthdayUserId; }
    public String getBirthdayUserName() { return birthdayUserName; }
    public LocalDate getBirthdayDate() { return birthdayDate; }
    public Integer getThresholdDays() { return thresholdDays; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getReadAt() { return readAt; }
}
