package ru.tsu.birthday.identity.domain;

import jakarta.persistence.*;
import java.util.UUID;

@Entity
@Table(name = "subscriptions",
       uniqueConstraints = @UniqueConstraint(columnNames = {"subscriber_id", "target_type", "target_id"}))
public class Subscription {
    public enum TargetType { USER, GROUP }

    @Id
    private UUID id;
    @Column(name = "subscriber_id", nullable = false)
    private UUID subscriberId;
    @Enumerated(EnumType.STRING)
    @Column(name = "target_type", nullable = false, length = 10)
    private TargetType targetType;
    @Column(name = "target_id", nullable = false)
    private UUID targetId;

    protected Subscription() {}

    public Subscription(UUID id, UUID subscriberId, TargetType targetType, UUID targetId) {
        this.id = id;
        this.subscriberId = subscriberId;
        this.targetType = targetType;
        this.targetId = targetId;
    }

    public UUID getSubscriberId() { return subscriberId; }
    public TargetType getTargetType() { return targetType; }
    public UUID getTargetId() { return targetId; }
}
