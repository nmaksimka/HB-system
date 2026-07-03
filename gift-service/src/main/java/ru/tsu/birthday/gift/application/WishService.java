package ru.tsu.birthday.gift.application;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.tsu.birthday.gift.domain.Wish;
import java.util.*;

@Service
@Transactional
public class WishService {
    private final WishRepository repository;
    public WishService(WishRepository repository) { this.repository = repository; }

    public Wish add(UUID userId, String title, String description, String link, int priority) {
        return repository.save(new Wish(UUID.randomUUID(), userId, title, description, link, priority));
    }
    @Transactional(readOnly = true)
    public List<Wish> byUser(UUID userId) { return repository.findByUserIdOrderByPriorityAsc(userId); }
    public void delete(UUID userId, UUID wishId) {
        var wish = repository.findById(wishId)
                .orElseThrow(() -> new NoSuchElementException("Wish not found: " + wishId));
        if (!wish.getUserId().equals(userId)) throw new IllegalArgumentException("Wish belongs to another user");
        repository.delete(wish);
    }
}
