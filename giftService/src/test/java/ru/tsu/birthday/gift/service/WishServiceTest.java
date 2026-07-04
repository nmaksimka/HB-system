package ru.tsu.birthday.gift.service;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import ru.tsu.birthday.gift.client.IdentityClient;
import ru.tsu.birthday.gift.entity.Wish;
import ru.tsu.birthday.gift.repository.WishRepository;

import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
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

        assertThatIllegalArgumentException()
                .isThrownBy(() -> service.delete(attackerId, wishId));
        verify(repository, never()).delete(wish);
    }

    @Test
    void reservesAnotherUsersWish() {
        var ownerId = UUID.randomUUID();
        var viewerId = UUID.randomUUID();
        var wishId = UUID.randomUUID();
        var wish = new Wish(wishId, ownerId, "Book", null, null, 1);
        when(identityClient.exists(ownerId)).thenReturn(true);
        when(identityClient.exists(viewerId)).thenReturn(true);
        when(repository.findById(wishId)).thenReturn(Optional.of(wish));

        var reserved = service.reserve(viewerId, ownerId, wishId);

        org.assertj.core.api.Assertions.assertThat(reserved.getReservedBy()).isEqualTo(viewerId);
        org.assertj.core.api.Assertions.assertThat(reserved.getReservedAt()).isEqualTo(clock.instant());
    }

    @Test
    void rejectsReservingOwnWish() {
        var ownerId = UUID.randomUUID();
        var wishId = UUID.randomUUID();
        var wish = new Wish(wishId, ownerId, "Book", null, null, 1);
        when(identityClient.exists(ownerId)).thenReturn(true);
        when(repository.findById(wishId)).thenReturn(Optional.of(wish));

        assertThatIllegalArgumentException()
                .isThrownBy(() -> service.reserve(ownerId, ownerId, wishId));
    }

    @Test
    void rejectsReservingWishTakenByAnotherUser() {
        var ownerId = UUID.randomUUID();
        var viewerId = UUID.randomUUID();
        var reserverId = UUID.randomUUID();
        var wishId = UUID.randomUUID();
        var wish = new Wish(wishId, ownerId, "Book", null, null, 1);
        wish.reserve(reserverId, clock.instant());
        when(identityClient.exists(ownerId)).thenReturn(true);
        when(identityClient.exists(viewerId)).thenReturn(true);
        when(repository.findById(wishId)).thenReturn(Optional.of(wish));

        assertThatIllegalArgumentException()
                .isThrownBy(() -> service.reserve(viewerId, ownerId, wishId));
    }
}
