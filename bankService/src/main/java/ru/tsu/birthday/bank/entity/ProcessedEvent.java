package ru.tsu.birthday.bank.entity;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "processed_events")
public class ProcessedEvent {
    @Id private UUID eventId;
    @Column(nullable = false) private Instant processedAt;

    protected ProcessedEvent() {}

    public ProcessedEvent(UUID eventId, Instant processedAt) {
        this.eventId = eventId;
        this.processedAt = processedAt;
    }
}
