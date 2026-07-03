package ru.tsu.birthday.identity.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.tsu.birthday.identity.entity.FriendGroup;
import java.util.UUID;

public interface GroupRepository extends JpaRepository<FriendGroup, UUID> {}
