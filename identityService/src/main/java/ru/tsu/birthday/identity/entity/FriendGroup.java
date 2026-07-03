package ru.tsu.birthday.identity.entity;

import jakarta.persistence.*;
import java.util.UUID;

@Entity
@Table(name = "friend_groups")
public class FriendGroup {
    @Id
    private UUID id;
    @Column(nullable = false, unique = true, length = 160)
    private String name;
    @Column(nullable = false)
    private UUID ownerId;

    protected FriendGroup() {}

    public FriendGroup(UUID id, String name, UUID ownerId) {
        this.id = id;
        this.name = name;
        this.ownerId = ownerId;
    }

    public UUID getId() { return id; }
    public String getName() { return name; }
    public UUID getOwnerId() { return ownerId; }
}
