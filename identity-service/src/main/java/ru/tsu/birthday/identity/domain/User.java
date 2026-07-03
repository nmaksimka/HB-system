package ru.tsu.birthday.identity.domain;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "users")
public class User {
    @Id
    private UUID id;
    @Column(nullable = false, length = 120)
    private String name;
    @Column(nullable = false)
    private LocalDate birthDate;

    protected User() {}

    public User(UUID id, String name, LocalDate birthDate) {
        this.id = id;
        this.name = name;
        this.birthDate = birthDate;
    }

    public UUID getId() { return id; }
    public String getName() { return name; }
    public LocalDate getBirthDate() { return birthDate; }
}
