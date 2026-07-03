package ru.tsu.birthday.identity.entity;

import jakarta.persistence.*;
import java.util.UUID;

@Entity
@Table(name = "group_memberships",
       uniqueConstraints = @UniqueConstraint(columnNames = {"group_id", "user_id"}))
public class GroupMembership {
    @Id
    private UUID id;
    @Column(name = "group_id", nullable = false)
    private UUID groupId;
    @Column(name = "user_id", nullable = false)
    private UUID userId;

    protected GroupMembership() {}

    public GroupMembership(UUID id, UUID groupId, UUID userId) {
        this.id = id;
        this.groupId = groupId;
        this.userId = userId;
    }

    public UUID getGroupId() { return groupId; }
    public UUID getUserId() { return userId; }
}
