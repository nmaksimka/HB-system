package ru.tsu.birthday.gift.service;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.UUID;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import ru.tsu.birthday.gift.client.IdentityClient;
import ru.tsu.birthday.gift.entity.Wish;
import ru.tsu.birthday.gift.repository.WishRepository;

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

    @Transactional(propagation = Propagation.NEVER)
    public Wish add(UUID userId, String title, String description, String link, int priority) {
        if (!identityClient.exists(userId)) {
            throw new NoSuchElementException("Create your profile before adding wishes");
        }
        return repository.save(new Wish(UUID.randomUUID(), userId, title.trim(), blankToNull(description),
                blankToNull(link), priority));
    }

    @Transactional(readOnly = true)
    public List<Wish> byUser(UUID userId) {
        return repository.findByUserIdOrderByPriorityAscTitleAsc(userId);
    }

    @Transactional(readOnly = true)
    public Wish wish(UUID wishId) {
        return repository.findById(wishId).orElseThrow(() -> new NoSuchElementException("Wish not found: " + wishId));
    }

    public void delete(UUID userId, UUID wishId) {
        var wish = wish(wishId);
        if (!wish.getUserId().equals(userId)) {
            throw new AccessDeniedException("Only the owner can delete a wish");
        }
        repository.delete(wish);
    }

    public Wish reserve(UUID viewerId, UUID userId, UUID wishId) {
        var wish = ownedWish(userId, wishId);
        if (wish.getUserId().equals(viewerId)) {
            throw new AccessDeniedException("You cannot reserve your own wish");
        }
        if (wish.isReserved() && !wish.isReservedBy(viewerId)) {
            throw new IllegalStateException("This gift is already reserved by someone else");
        }
        wish.reserve(viewerId, Instant.now(clock));
        return repository.saveAndFlush(wish);
    }

    public Wish cancelReservation(UUID viewerId, UUID userId, UUID wishId) {
        var wish = ownedWish(userId, wishId);
        if (!wish.isReserved()) {
            return wish;
        }
        if (!wish.isReservedBy(viewerId)) {
            throw new AccessDeniedException("Only the person who reserved the gift can cancel it");
        }
        wish.cancelReservation();
        return repository.saveAndFlush(wish);
    }

    private Wish ownedWish(UUID userId, UUID wishId) {
        var wish = wish(wishId);
        if (!wish.getUserId().equals(userId)) {
            throw new NoSuchElementException("Wish not found: " + wishId);
        }
        return wish;
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
