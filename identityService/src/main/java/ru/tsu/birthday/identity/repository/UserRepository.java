package ru.tsu.birthday.identity.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.tsu.birthday.identity.entity.User;
import java.util.UUID;

public interface UserRepository extends JpaRepository<User, UUID> {}
