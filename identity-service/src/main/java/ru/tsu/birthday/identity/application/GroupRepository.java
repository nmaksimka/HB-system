package ru.tsu.birthday.identity.application;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.tsu.birthday.identity.domain.FriendGroup;
import java.util.UUID;

public interface GroupRepository extends JpaRepository<FriendGroup, UUID> {}
