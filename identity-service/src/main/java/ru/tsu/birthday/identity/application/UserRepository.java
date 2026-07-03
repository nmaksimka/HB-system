package ru.tsu.birthday.identity.application;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.tsu.birthday.identity.domain.User;
import java.util.UUID;

public interface UserRepository extends JpaRepository<User, UUID> {}
