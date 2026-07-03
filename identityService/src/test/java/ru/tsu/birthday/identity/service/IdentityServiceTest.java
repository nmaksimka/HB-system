package ru.tsu.birthday.identity.service;

import org.junit.jupiter.api.Test;
import java.time.LocalDate;
import java.time.Clock;
import java.util.Optional;
import java.util.UUID;
import ru.tsu.birthday.identity.client.GiftClient;
import ru.tsu.birthday.identity.entity.Subscription;
import ru.tsu.birthday.identity.entity.User;
import ru.tsu.birthday.identity.repository.GroupRepository;
import ru.tsu.birthday.identity.repository.MembershipRepository;
import ru.tsu.birthday.identity.repository.SubscriptionRepository;
import ru.tsu.birthday.identity.repository.UserRepository;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class IdentityServiceTest {
    @Test
    void calculatesBirthdayAcrossYearBoundary() {
        var today = LocalDate.of(2026, 12, 30);
        assertThat(IdentityService.daysUntil(LocalDate.of(2000, 1, 2), today)).isEqualTo(3);
    }

    @Test
    void treatsTodayAsUpcomingBirthday() {
        var today = LocalDate.of(2026, 7, 3);
        assertThat(IdentityService.daysUntil(LocalDate.of(2000, 7, 3), today)).isZero();
    }

    @Test
    void handlesLeapDayInNonLeapYear() {
        var today = LocalDate.of(2026, 2, 27);
        assertThat(IdentityService.daysUntil(LocalDate.of(2000, 2, 29), today)).isEqualTo(1);
    }

    @Test
    void returnsExistingSubscriptionWhenSubscribeIsRepeated() {
        var users = mock(UserRepository.class);
        var subscriptions = mock(SubscriptionRepository.class);
        var service = new IdentityService(
                users,
                mock(GroupRepository.class),
                mock(MembershipRepository.class),
                subscriptions,
                mock(GiftClient.class),
                Clock.systemUTC()
        );
        var subscriberId = UUID.randomUUID();
        var targetId = UUID.randomUUID();
        var existing = new Subscription(UUID.randomUUID(), subscriberId, Subscription.TargetType.USER, targetId);
        when(users.findById(subscriberId)).thenReturn(Optional.of(new User(subscriberId, "Max", LocalDate.EPOCH)));
        when(users.findById(targetId)).thenReturn(Optional.of(new User(targetId, "Anna", LocalDate.EPOCH)));
        when(subscriptions.findBySubscriberIdAndTargetTypeAndTargetId(
                subscriberId, Subscription.TargetType.USER, targetId)).thenReturn(Optional.of(existing));

        assertThat(service.subscribe(subscriberId, Subscription.TargetType.USER, targetId)).isSameAs(existing);
        verify(subscriptions, never()).save(any());
    }
}
