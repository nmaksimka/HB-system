package ru.tsu.birthday.gift.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.tsu.birthday.gift.client.IdentityClient;
import ru.tsu.birthday.gift.entity.Wish;
import ru.tsu.birthday.gift.repository.WishRepository;
import java.time.Clock;
import java.time.Instant;
import java.util.*;

@Service
@Transactional
public class WishService {
    private final WishRepository repository;
    private final IdentityClient identityClient;
    private final Clock clock;

    public WishService(WishRepository repository, IdentityClient identityClient, Clock clock) {
        this.repository = repository;
        this.identityClient = identityClient;
        this.clock = clock;
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

    public Wish reserve(UUID viewerId, UUID userId, UUID wishId) {
        requireUser(viewerId);
        var wish = ownedWish(userId, wishId);
        if (wish.getUserId().equals(viewerId)) {
            throw new IllegalArgumentException("You cannot reserve your own wish");
        }
        if (wish.getReservedBy() != null && !wish.getReservedBy().equals(viewerId)) {
            throw new IllegalArgumentException("Wish is already reserved");
        }
        wish.reserve(viewerId, Instant.now(clock));
        return wish;
    }

    public Wish cancelReservation(UUID viewerId, UUID userId, UUID wishId) {
        requireUser(viewerId);
        var wish = ownedWish(userId, wishId);
        if (wish.getReservedBy() == null) {
            return wish;
        }
        if (!wish.getReservedBy().equals(viewerId)) {
            throw new IllegalArgumentException("Only the reserver can cancel this reservation");
        }
        wish.cancelReservation();
        return wish;
    }

    private Wish ownedWish(UUID userId, UUID wishId) {
        requireUser(userId);
        var wish = repository.findById(wishId)
                .orElseThrow(() -> new NoSuchElementException("Wish not found: " + wishId));
        if (!wish.getUserId().equals(userId)) {
            throw new IllegalArgumentException("Wish belongs to another user");
        }
        return wish;
    }

    private void requireUser(UUID userId) {
        if (!identityClient.exists(userId)) {
            throw new NoSuchElementException("User not found: " + userId);
        }
    }
}
