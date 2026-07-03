package ru.tsu.birthday.gift.application;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.tsu.birthday.gift.domain.Wish;
import java.util.*;

public interface WishRepository extends JpaRepository<Wish, UUID> {
    List<Wish> findByUserIdOrderByPriorityAsc(UUID userId);
}
