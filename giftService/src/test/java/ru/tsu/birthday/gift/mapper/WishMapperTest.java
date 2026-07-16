package ru.tsu.birthday.gift.mapper;

import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import ru.tsu.birthday.gift.entity.Wish;

import static org.assertj.core.api.Assertions.assertThat;

class WishMapperTest {
    private final WishMapper mapper = new WishMapper();
    private final UUID ownerId = UUID.randomUUID();
    private final UUID reserverId = UUID.randomUUID();
    private final Wish wish = new Wish(UUID.randomUUID(), ownerId, "Book", "Hardcover", "https://shop.example", 2);

    @Test
    void hidesReservationFromOwner() {
        wish.reserve(reserverId, Instant.parse("2026-01-01T10:00:00Z"));

        var response = mapper.toResponse(wish, ownerId);

        assertThat(response.reserved()).isFalse();
        assertThat(response.reservedByMe()).isFalse();
    }

    @Test
    void showsReservationToOtherViewer() {
        wish.reserve(reserverId, Instant.parse("2026-01-01T10:00:00Z"));

        var response = mapper.toResponse(wish, UUID.randomUUID());

        assertThat(response.reserved()).isTrue();
        assertThat(response.reservedByMe()).isFalse();
    }

    @Test
    void marksReservationMadeByViewer() {
        wish.reserve(reserverId, Instant.parse("2026-01-01T10:00:00Z"));

        var response = mapper.toResponse(wish, reserverId);

        assertThat(response.reserved()).isTrue();
        assertThat(response.reservedByMe()).isTrue();
    }

    @Test
    void showsFreeWishAsNotReserved() {
        var response = mapper.toResponse(wish, UUID.randomUUID());

        assertThat(response.reserved()).isFalse();
        assertThat(response.reservedByMe()).isFalse();
    }

    @Test
    void copiesWishFields() {
        var response = mapper.toResponse(wish, UUID.randomUUID());

        assertThat(response.id()).isEqualTo(wish.getId());
        assertThat(response.userId()).isEqualTo(ownerId);
        assertThat(response.title()).isEqualTo("Book");
        assertThat(response.description()).isEqualTo("Hardcover");
        assertThat(response.link()).isEqualTo("https://shop.example");
        assertThat(response.priority()).isEqualTo(2);
    }
}
