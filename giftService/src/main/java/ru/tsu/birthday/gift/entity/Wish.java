package ru.tsu.birthday.gift.entity;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "wishes")
public class Wish {
    @Id private UUID id;
    @Column(nullable = false) private UUID userId;
    @Column(nullable = false, length = 160) private String title;
    @Column(length = 1000) private String description;
    @Column(length = 500) private String link;
    @Column(nullable = false) private Integer priority;
    private UUID reservedBy;
    private Instant reservedAt;
    @Version private long version;

    protected Wish() {}

    public Wish(UUID id, UUID userId, String title, String description, String link, Integer priority) {
        this.id = id;
        this.userId = userId;
        this.title = title;
        this.description = description;
        this.link = link;
        this.priority = priority;
    }

    public boolean isReserved() { return reservedBy != null; }

    public boolean isReservedBy(UUID viewerId) { return viewerId.equals(reservedBy); }

    public void reserve(UUID viewerId, Instant timestamp) {
        reservedBy = viewerId;
        reservedAt = timestamp;
    }

    public void cancelReservation() {
        reservedBy = null;
        reservedAt = null;
    }

    public UUID getId() { return id; }
    public UUID getUserId() { return userId; }
    public String getTitle() { return title; }
    public String getDescription() { return description; }
    public String getLink() { return link; }
    public Integer getPriority() { return priority; }
    public UUID getReservedBy() { return reservedBy; }
    public Instant getReservedAt() { return reservedAt; }
}
