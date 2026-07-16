package ru.tsu.birthday.fundraiser.entity;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class FundraiserTest {
    private final Instant created = Instant.parse("2026-01-01T10:00:00Z");
    private final Instant later = Instant.parse("2026-01-02T10:00:00Z");
    private final Fundraiser fundraiser = new Fundraiser(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
            UUID.randomUUID(), "Book", new BigDecimal("100.00"), created);

    @Test
    void startsActiveWithZeroCollected() {
        assertThat(fundraiser.isActive()).isTrue();
        assertThat(fundraiser.getCollectedAmount()).isEqualByComparingTo("0");
        assertThat(fundraiser.getClosedAt()).isNull();
    }

    @Test
    void staysActiveWhileTargetIsNotReached() {
        var closed = fundraiser.contribute(new BigDecimal("99.99"), later);

        assertThat(closed).isFalse();
        assertThat(fundraiser.isActive()).isTrue();
        assertThat(fundraiser.getCollectedAmount()).isEqualByComparingTo("99.99");
    }

    @Test
    void completesWhenTargetIsReachedExactly() {
        var closed = fundraiser.contribute(new BigDecimal("100.00"), later);

        assertThat(closed).isTrue();
        assertThat(fundraiser.getStatus()).isEqualTo(FundraiserStatus.COMPLETED);
        assertThat(fundraiser.getClosedAt()).isEqualTo(later);
    }

    @Test
    void completesAfterSeveralContributions() {
        fundraiser.contribute(new BigDecimal("60.00"), later);
        var closed = fundraiser.contribute(new BigDecimal("40.00"), later);

        assertThat(closed).isTrue();
        assertThat(fundraiser.getCollectedAmount()).isEqualByComparingTo("100.00");
    }

    @Test
    void completesWhenTargetIsExceeded() {
        var closed = fundraiser.contribute(new BigDecimal("150.00"), later);

        assertThat(closed).isTrue();
        assertThat(fundraiser.getStatus()).isEqualTo(FundraiserStatus.COMPLETED);
    }

    @Test
    void cancelClosesFundraiser() {
        fundraiser.cancel(later);

        assertThat(fundraiser.isActive()).isFalse();
        assertThat(fundraiser.getStatus()).isEqualTo(FundraiserStatus.CANCELLED);
        assertThat(fundraiser.getClosedAt()).isEqualTo(later);
    }
}
