package ru.tsu.birthday.gift.service;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;
import ru.tsu.birthday.gift.client.IdentityClient;
import ru.tsu.birthday.gift.entity.Wish;
import ru.tsu.birthday.gift.repository.WishRepository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class WishServiceTest {
    private final WishRepository repository = mock(WishRepository.class);
    private final IdentityClient identityClient = mock(IdentityClient.class);
    private final Clock clock = Clock.fixed(Instant.parse("2026-01-01T10:00:00Z"), ZoneOffset.UTC);
    private final WishService service = new WishService(repository, identityClient, clock);

    @Test
    void checksUserExistsBeforeAddingWish() {
        var userId = UUID.randomUUID();
        when(identityClient.exists(userId)).thenReturn(false);

        assertThatThrownBy(() -> service.add(userId, "Book", null, null, 1))
                .isInstanceOf(NoSuchElementException.class);
        verify(repository, never()).save(any());
    }

    @Test
    void trimsTitleAndTurnsBlankFieldsIntoNull() {
        var userId = UUID.randomUUID();
        when(identityClient.exists(userId)).thenReturn(true);
        when(repository.save(any(Wish.class))).thenAnswer(call -> call.getArgument(0));

        var wish = service.add(userId, "  Book  ", "   ", "", 3);

        assertThat(wish.getTitle()).isEqualTo("Book");
        assertThat(wish.getDescription()).isNull();
        assertThat(wish.getLink()).isNull();
        assertThat(wish.getUserId()).isEqualTo(userId);
        assertThat(wish.getPriority()).isEqualTo(3);
    }

    @Test
    void trimsDescriptionAndLink() {
        var userId = UUID.randomUUID();
        when(identityClient.exists(userId)).thenReturn(true);
        when(repository.save(any(Wish.class))).thenAnswer(call -> call.getArgument(0));

        var wish = service.add(userId, "Book", " Hardcover ", " https://shop.example/book ", 1);

        assertThat(wish.getDescription()).isEqualTo("Hardcover");
        assertThat(wish.getLink()).isEqualTo("https://shop.example/book");
    }

    @Test
    void allowsOwnerToDeleteOwnWish() {
        var userId = UUID.randomUUID();
        var wishId = UUID.randomUUID();
        var wish = new Wish(wishId, userId, "Book", null, null, 1);
        when(repository.findById(wishId)).thenReturn(Optional.of(wish));

        assertThatNoException().isThrownBy(() -> service.delete(userId, wishId));
        verify(repository).delete(wish);
    }

    @Test
    void rejectsDeletingAnotherUsersWish() {
        var ownerId = UUID.randomUUID();
        var attackerId = UUID.randomUUID();
        var wishId = UUID.randomUUID();
        var wish = new Wish(wishId, ownerId, "Book", null, null, 1);
        when(repository.findById(wishId)).thenReturn(Optional.of(wish));

        assertThatThrownBy(() -> service.delete(attackerId, wishId))
                .isInstanceOf(AccessDeniedException.class);
        verify(repository, never()).delete(wish);
    }

    @Test
    void rejectsDeletingMissingWish() {
        var wishId = UUID.randomUUID();
        when(repository.findById(wishId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.delete(UUID.randomUUID(), wishId))
                .isInstanceOf(NoSuchElementException.class);
    }

    @Test
    void reservesAnotherUsersWish() {
        var ownerId = UUID.randomUUID();
        var viewerId = UUID.randomUUID();
        var wishId = UUID.randomUUID();
        var wish = new Wish(wishId, ownerId, "Book", null, null, 1);
        when(repository.findById(wishId)).thenReturn(Optional.of(wish));
        when(repository.saveAndFlush(wish)).thenReturn(wish);

        var reserved = service.reserve(viewerId, ownerId, wishId);

        assertThat(reserved.getReservedBy()).isEqualTo(viewerId);
        assertThat(reserved.getReservedAt()).isEqualTo(clock.instant());
    }

    @Test
    void rejectsReservingOwnWish() {
        var ownerId = UUID.randomUUID();
        var wishId = UUID.randomUUID();
        var wish = new Wish(wishId, ownerId, "Book", null, null, 1);
        when(repository.findById(wishId)).thenReturn(Optional.of(wish));

        assertThatThrownBy(() -> service.reserve(ownerId, ownerId, wishId))
                .isInstanceOf(AccessDeniedException.class);
        verify(repository, never()).saveAndFlush(any());
    }

    @Test
    void rejectsReservingWishTakenByAnotherUser() {
        var ownerId = UUID.randomUUID();
        var viewerId = UUID.randomUUID();
        var reserverId = UUID.randomUUID();
        var wishId = UUID.randomUUID();
        var wish = new Wish(wishId, ownerId, "Book", null, null, 1);
        wish.reserve(reserverId, clock.instant());
        when(repository.findById(wishId)).thenReturn(Optional.of(wish));

        assertThatThrownBy(() -> service.reserve(viewerId, ownerId, wishId))
                .isInstanceOf(IllegalStateException.class);
        assertThat(wish.getReservedBy()).isEqualTo(reserverId);
        verify(repository, never()).saveAndFlush(any());
    }

    @Test
    void allowsRepeatedReservationBySamePerson() {
        var ownerId = UUID.randomUUID();
        var viewerId = UUID.randomUUID();
        var wishId = UUID.randomUUID();
        var wish = new Wish(wishId, ownerId, "Book", null, null, 1);
        wish.reserve(viewerId, Instant.parse("2025-12-31T10:00:00Z"));
        when(repository.findById(wishId)).thenReturn(Optional.of(wish));
        when(repository.saveAndFlush(wish)).thenReturn(wish);

        var reserved = service.reserve(viewerId, ownerId, wishId);

        assertThat(reserved.getReservedBy()).isEqualTo(viewerId);
    }

    @Test
    void rejectsReservingWishUnderAnotherOwnersId() {
        var ownerId = UUID.randomUUID();
        var wishId = UUID.randomUUID();
        var wish = new Wish(wishId, ownerId, "Book", null, null, 1);
        when(repository.findById(wishId)).thenReturn(Optional.of(wish));

        assertThatThrownBy(() -> service.reserve(UUID.randomUUID(), UUID.randomUUID(), wishId))
                .isInstanceOf(NoSuchElementException.class);
        verify(repository, never()).saveAndFlush(any());
    }

    @Test
    void cancelsOwnReservation() {
        var ownerId = UUID.randomUUID();
        var viewerId = UUID.randomUUID();
        var wishId = UUID.randomUUID();
        var wish = new Wish(wishId, ownerId, "Book", null, null, 1);
        wish.reserve(viewerId, clock.instant());
        when(repository.findById(wishId)).thenReturn(Optional.of(wish));
        when(repository.saveAndFlush(wish)).thenReturn(wish);

        var result = service.cancelReservation(viewerId, ownerId, wishId);

        assertThat(result.isReserved()).isFalse();
        assertThat(result.getReservedAt()).isNull();
    }

    @Test
    void rejectsCancellingAnotherUsersReservation() {
        var ownerId = UUID.randomUUID();
        var reserverId = UUID.randomUUID();
        var wishId = UUID.randomUUID();
        var wish = new Wish(wishId, ownerId, "Book", null, null, 1);
        wish.reserve(reserverId, clock.instant());
        when(repository.findById(wishId)).thenReturn(Optional.of(wish));

        assertThatThrownBy(() -> service.cancelReservation(UUID.randomUUID(), ownerId, wishId))
                .isInstanceOf(AccessDeniedException.class);
        assertThat(wish.getReservedBy()).isEqualTo(reserverId);
        verify(repository, never()).saveAndFlush(any());
    }

    @Test
    void cancellingFreeWishDoesNothing() {
        var ownerId = UUID.randomUUID();
        var wishId = UUID.randomUUID();
        var wish = new Wish(wishId, ownerId, "Book", null, null, 1);
        when(repository.findById(wishId)).thenReturn(Optional.of(wish));

        var result = service.cancelReservation(UUID.randomUUID(), ownerId, wishId);

        assertThat(result).isSameAs(wish);
        verify(repository, never()).saveAndFlush(any());
    }
}
