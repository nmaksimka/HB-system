package ru.tsu.birthday.identity.service;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import ru.tsu.birthday.identity.entity.FriendGroup;
import ru.tsu.birthday.identity.entity.GroupMembership;
import ru.tsu.birthday.identity.entity.Subscription;
import ru.tsu.birthday.identity.entity.User;
import ru.tsu.birthday.identity.repository.GroupRepository;
import ru.tsu.birthday.identity.repository.MembershipRepository;
import ru.tsu.birthday.identity.repository.SubscriptionRepository;
import ru.tsu.birthday.identity.repository.UserRepository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class IdentityServiceTest {
    private final UserRepository users = mock(UserRepository.class);
    private final GroupRepository groups = mock(GroupRepository.class);
    private final MembershipRepository memberships = mock(MembershipRepository.class);
    private final SubscriptionRepository subscriptions = mock(SubscriptionRepository.class);
    private final Clock clock = Clock.fixed(Instant.parse("2026-07-01T10:00:00Z"), ZoneOffset.UTC);
    private final IdentityService service = new IdentityService(users, groups, memberships, subscriptions, clock);

    @Test
    void returnsExistingSubscriptionWhenSubscribeIsRepeated() {
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

    @Test
    void savesNewSubscriptionToUser() {
        var subscriberId = UUID.randomUUID();
        var targetId = UUID.randomUUID();
        when(users.findById(subscriberId)).thenReturn(Optional.of(new User(subscriberId, "Max", LocalDate.EPOCH)));
        when(users.findById(targetId)).thenReturn(Optional.of(new User(targetId, "Anna", LocalDate.EPOCH)));
        when(subscriptions.findBySubscriberIdAndTargetTypeAndTargetId(
                subscriberId, Subscription.TargetType.USER, targetId)).thenReturn(Optional.empty());
        when(subscriptions.save(any(Subscription.class))).thenAnswer(call -> call.getArgument(0));

        var created = service.subscribe(subscriberId, Subscription.TargetType.USER, targetId);

        assertThat(created.getSubscriberId()).isEqualTo(subscriberId);
        assertThat(created.getTargetId()).isEqualTo(targetId);
        assertThat(created.getTargetType()).isEqualTo(Subscription.TargetType.USER);
    }

    @Test
    void rejectsSubscribingToYourself() {
        var userId = UUID.randomUUID();
        when(users.findById(userId)).thenReturn(Optional.of(new User(userId, "Max", LocalDate.EPOCH)));

        assertThatIllegalArgumentException()
                .isThrownBy(() -> service.subscribe(userId, Subscription.TargetType.USER, userId));
        verify(subscriptions, never()).save(any());
    }

    @Test
    void rejectsSubscribingToMissingGroup() {
        var subscriberId = UUID.randomUUID();
        var groupId = UUID.randomUUID();
        when(users.findById(subscriberId)).thenReturn(Optional.of(new User(subscriberId, "Max", LocalDate.EPOCH)));
        when(groups.existsById(groupId)).thenReturn(false);

        assertThatThrownBy(() -> service.subscribe(subscriberId, Subscription.TargetType.GROUP, groupId))
                .isInstanceOf(NoSuchElementException.class);
        verify(subscriptions, never()).save(any());
    }

    @Test
    void rejectsSubscribingToMissingUser() {
        var subscriberId = UUID.randomUUID();
        var targetId = UUID.randomUUID();
        when(users.findById(subscriberId)).thenReturn(Optional.of(new User(subscriberId, "Max", LocalDate.EPOCH)));
        when(users.findById(targetId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.subscribe(subscriberId, Subscription.TargetType.USER, targetId))
                .isInstanceOf(NoSuchElementException.class);
    }

    @Test
    void expandsGroupSubscriptionToMembersWithoutSubscriber() {
        var subscriberId = UUID.randomUUID();
        var annaId = UUID.randomUUID();
        var groupId = UUID.randomUUID();
        var anna = new User(annaId, "Anna", LocalDate.of(2000, 7, 5));
        when(subscriptions.findBySubscriberId(subscriberId)).thenReturn(List.of(
                new Subscription(UUID.randomUUID(), subscriberId, Subscription.TargetType.GROUP, groupId)));
        when(memberships.findByGroupId(groupId)).thenReturn(List.of(
                new GroupMembership(UUID.randomUUID(), groupId, subscriberId),
                new GroupMembership(UUID.randomUUID(), groupId, annaId)));
        when(users.findAllById(Set.of(annaId))).thenReturn(List.of(anna));

        var result = service.upcomingForSubscriber(subscriberId, 30);

        assertThat(result).hasSize(1);
        assertThat(result.getFirst().id()).isEqualTo(annaId);
        assertThat(result.getFirst().daysLeft()).isEqualTo(4);
        assertThat(result.getFirst().nextBirthday()).isEqualTo(LocalDate.of(2026, 7, 5));
    }

    @Test
    void excludesSubscriberWhoIsDirectTargetToo() {
        var subscriberId = UUID.randomUUID();
        when(subscriptions.findBySubscriberId(subscriberId)).thenReturn(List.of(
                new Subscription(UUID.randomUUID(), subscriberId, Subscription.TargetType.USER, subscriberId)));
        when(users.findAllById(Set.of())).thenReturn(List.of());

        assertThat(service.upcomingForSubscriber(subscriberId, 30)).isEmpty();
        verify(users).findAllById(Set.of());
    }

    @Test
    void sortsUpcomingBirthdaysByDaysLeftAndFiltersByDays() {
        var subscriberId = UUID.randomUUID();
        var soon = new User(UUID.randomUUID(), "Soon", LocalDate.of(1990, 7, 3));
        var later = new User(UUID.randomUUID(), "Later", LocalDate.of(1990, 7, 20));
        var far = new User(UUID.randomUUID(), "Far", LocalDate.of(1990, 12, 1));
        when(subscriptions.findBySubscriberId(subscriberId)).thenReturn(List.of(
                new Subscription(UUID.randomUUID(), subscriberId, Subscription.TargetType.USER, far.getId()),
                new Subscription(UUID.randomUUID(), subscriberId, Subscription.TargetType.USER, later.getId()),
                new Subscription(UUID.randomUUID(), subscriberId, Subscription.TargetType.USER, soon.getId())));
        when(users.findAllById(any())).thenReturn(List.of(far, later, soon));

        var result = service.upcomingForSubscriber(subscriberId, 30);

        assertThat(result).extracting(r -> r.name()).containsExactly("Soon", "Later");
        assertThat(result).extracting(r -> r.daysLeft()).containsExactly(2, 19);
    }

    @Test
    void includesBirthdayExactlyOnDaysBoundary() {
        var subscriberId = UUID.randomUUID();
        var user = new User(UUID.randomUUID(), "Edge", LocalDate.of(1990, 7, 31));
        when(subscriptions.findBySubscriberId(subscriberId)).thenReturn(List.of(
                new Subscription(UUID.randomUUID(), subscriberId, Subscription.TargetType.USER, user.getId())));
        when(users.findAllById(any())).thenReturn(List.of(user));

        assertThat(service.upcomingForSubscriber(subscriberId, 30)).hasSize(1);
        assertThat(service.upcomingForSubscriber(subscriberId, 29)).isEmpty();
    }

    @Test
    void cannotDiscussYourOwnBirthday() {
        var userId = UUID.randomUUID();
        when(users.existsById(userId)).thenReturn(true);

        assertThat(service.canDiscuss(userId, userId)).isFalse();
    }

    @Test
    void cannotDiscussMissingUser() {
        var subjectId = UUID.randomUUID();
        when(users.existsById(subjectId)).thenReturn(false);

        assertThat(service.canDiscuss(subjectId, UUID.randomUUID())).isFalse();
    }

    @Test
    void canDiscussSubscribedFriend() {
        var subjectId = UUID.randomUUID();
        var viewerId = UUID.randomUUID();
        when(users.existsById(subjectId)).thenReturn(true);
        when(subscriptions.findBySubscriberIdAndTargetTypeAndTargetId(
                viewerId, Subscription.TargetType.USER, subjectId)).thenReturn(Optional.of(
                new Subscription(UUID.randomUUID(), viewerId, Subscription.TargetType.USER, subjectId)));

        assertThat(service.canDiscuss(subjectId, viewerId)).isTrue();
    }

    @Test
    void canDiscussUserFromCommonGroup() {
        var subjectId = UUID.randomUUID();
        var viewerId = UUID.randomUUID();
        var groupId = UUID.randomUUID();
        when(users.existsById(subjectId)).thenReturn(true);
        when(subscriptions.findBySubscriberIdAndTargetTypeAndTargetId(
                viewerId, Subscription.TargetType.USER, subjectId)).thenReturn(Optional.empty());
        when(memberships.findByUserId(viewerId)).thenReturn(List.of(
                new GroupMembership(UUID.randomUUID(), groupId, viewerId)));
        when(memberships.findByUserId(subjectId)).thenReturn(List.of(
                new GroupMembership(UUID.randomUUID(), groupId, subjectId)));

        assertThat(service.canDiscuss(subjectId, viewerId)).isTrue();
    }

    @Test
    void cannotDiscussStranger() {
        var subjectId = UUID.randomUUID();
        var viewerId = UUID.randomUUID();
        when(users.existsById(subjectId)).thenReturn(true);
        when(subscriptions.findBySubscriberIdAndTargetTypeAndTargetId(
                viewerId, Subscription.TargetType.USER, subjectId)).thenReturn(Optional.empty());
        when(memberships.findByUserId(viewerId)).thenReturn(List.of(
                new GroupMembership(UUID.randomUUID(), UUID.randomUUID(), viewerId)));
        when(memberships.findByUserId(subjectId)).thenReturn(List.of(
                new GroupMembership(UUID.randomUUID(), UUID.randomUUID(), subjectId)));

        assertThat(service.canDiscuss(subjectId, viewerId)).isFalse();
    }

    @Test
    void rejectsGroupWithDuplicateName() {
        var ownerId = UUID.randomUUID();
        when(users.findById(ownerId)).thenReturn(Optional.of(new User(ownerId, "Max", LocalDate.EPOCH)));
        when(groups.existsByNameIgnoreCase("Friends")).thenReturn(true);

        assertThatIllegalStateException().isThrownBy(() -> service.createGroup(" Friends ", ownerId));
        verify(groups, never()).save(any());
        verify(memberships, never()).save(any());
    }

    @Test
    void makesGroupCreatorItsMember() {
        var ownerId = UUID.randomUUID();
        when(users.findById(ownerId)).thenReturn(Optional.of(new User(ownerId, "Max", LocalDate.EPOCH)));
        when(groups.existsByNameIgnoreCase("Friends")).thenReturn(false);
        when(groups.save(any(FriendGroup.class))).thenAnswer(call -> call.getArgument(0));

        var group = service.createGroup(" Friends ", ownerId);

        var membership = ArgumentCaptor.forClass(GroupMembership.class);
        verify(memberships).save(membership.capture());
        assertThat(group.getName()).isEqualTo("Friends");
        assertThat(group.getOwnerId()).isEqualTo(ownerId);
        assertThat(membership.getValue().getGroupId()).isEqualTo(group.getId());
        assertThat(membership.getValue().getUserId()).isEqualTo(ownerId);
    }

    @Test
    void rejectsGroupFromUnknownOwner() {
        var ownerId = UUID.randomUUID();
        when(users.findById(ownerId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.createGroup("Friends", ownerId))
                .isInstanceOf(NoSuchElementException.class);
        verify(groups, never()).save(any());
    }
}
