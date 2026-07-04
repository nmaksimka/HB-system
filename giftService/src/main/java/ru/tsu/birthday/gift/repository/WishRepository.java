package ru.tsu.birthday.gift.repository;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import ru.tsu.birthday.gift.entity.Wish;

public interface WishRepository extends JpaRepository<Wish, UUID> {
    List<Wish> findByUserIdOrderByPriorityAscTitleAsc(UUID userId);
}
