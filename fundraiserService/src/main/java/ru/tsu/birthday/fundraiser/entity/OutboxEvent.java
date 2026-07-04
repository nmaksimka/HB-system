package ru.tsu.birthday.fundraiser.entity;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "outbox_events")
public class OutboxEvent {
    @Id private UUID id;
    @Column(nullable = false, length = 100) private String topic;
    @Column(nullable = false) private UUID aggregateId;
    @Column(nullable = false, columnDefinition = "text") private String payload;
    @Column(nullable = false) private Instant createdAt;
    private Instant sentAt;
    @Column(nullable = false) private int attempts;

    protected OutboxEvent() {}

    public OutboxEvent(UUID id, String topic, UUID aggregateId, String payload, Instant createdAt) {
        this.id = id;
        this.topic = topic;
        this.aggregateId = aggregateId;
        this.payload = payload;
        this.createdAt = createdAt;
    }

    public void markSent(Instant timestamp) { sentAt = timestamp; }
    public void markFailed() { attempts++; }

    public UUID getId() { return id; }
    public String getTopic() { return topic; }
    public UUID getAggregateId() { return aggregateId; }
    public String getPayload() { return payload; }
    public Instant getSentAt() { return sentAt; }
    public int getAttempts() { return attempts; }
}
