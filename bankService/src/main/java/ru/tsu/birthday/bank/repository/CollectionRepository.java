package ru.tsu.birthday.bank.repository;

import java.util.Optional;
import java.util.UUID;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import ru.tsu.birthday.bank.entity.Collection;

public interface CollectionRepository extends JpaRepository<Collection, UUID> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select c from Collection c where c.fundraiserId = :fundraiserId")
    Optional<Collection> lockById(UUID fundraiserId);
}
