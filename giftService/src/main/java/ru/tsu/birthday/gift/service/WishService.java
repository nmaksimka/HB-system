package ru.tsu.birthday.gift.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.tsu.birthday.gift.client.IdentityClient;
import ru.tsu.birthday.gift.entity.Wish;
import ru.tsu.birthday.gift.repository.WishRepository;
import java.util.*;

@Service
@Transactional
public class WishService {
    private final WishRepository repository;
    private final IdentityClient identityClient;

    public WishService(WishRepository repository, IdentityClient identityClient) {
        this.repository = repository;
        this.identityClient = identityClient;
    }

    public Wish add(UUID userId, String title, String description, String link, int priority) {
        requireUser(userId);
        return repository.save(new Wish(UUID.randomUUID(), userId, title, description, link, priority));
    }
    @Transactional(readOnly = true)
    public List<Wish> byUser(UUID userId) {
        requireUser(userId);
        return repository.findByUserIdOrderByPriorityAsc(userId);
    }
    public void delete(UUID userId, UUID wishId) {
        var wish = repository.findById(wishId)
                .orElseThrow(() -> new NoSuchElementException("Wish not found: " + wishId));
        if (!wish.getUserId().equals(userId)) throw new IllegalArgumentException("Wish belongs to another user");
        repository.delete(wish);
    }

    private void requireUser(UUID userId) {
        if (!identityClient.exists(userId)) {
            throw new NoSuchElementException("User not found: " + userId);
        }
    }
}
