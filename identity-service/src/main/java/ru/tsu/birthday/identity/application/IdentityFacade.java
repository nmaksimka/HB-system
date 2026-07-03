package ru.tsu.birthday.identity.application;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.tsu.birthday.identity.adapter.out.GiftClient;
import ru.tsu.birthday.identity.domain.*;
import java.time.*;
import java.util.*;

@Service
@Transactional
public class IdentityFacade {
    private final UserRepository users;
    private final GroupRepository groups;
    private final MembershipRepository memberships;
    private final SubscriptionRepository subscriptions;
    private final GiftClient giftClient;

    public IdentityFacade(UserRepository users, GroupRepository groups,
                          MembershipRepository memberships, SubscriptionRepository subscriptions,
                          GiftClient giftClient) {
        this.users = users;
        this.groups = groups;
        this.memberships = memberships;
        this.subscriptions = subscriptions;
        this.giftClient = giftClient;
    }

    public User createUser(String name, LocalDate birthDate) {
        return users.save(new User(UUID.randomUUID(), name, birthDate));
    }

    @Transactional(readOnly = true)
    public List<User> users() { return users.findAll(); }

    @Transactional(readOnly = true)
    public User user(UUID id) {
        return users.findById(id).orElseThrow(() -> new NoSuchElementException("User not found: " + id));
    }

    public FriendGroup createGroup(String name, UUID ownerId) {
        user(ownerId);
        var group = groups.save(new FriendGroup(UUID.randomUUID(), name, ownerId));
        memberships.save(new GroupMembership(UUID.randomUUID(), group.getId(), ownerId));
        return group;
    }

    public void join(UUID groupId, UUID userId) {
        if (!groups.existsById(groupId)) throw new NoSuchElementException("Group not found: " + groupId);
        user(userId);
        if (!memberships.existsByGroupIdAndUserId(groupId, userId)) {
            memberships.save(new GroupMembership(UUID.randomUUID(), groupId, userId));
        }
    }

    @Transactional(readOnly = true)
    public List<FriendGroup> groups() { return groups.findAll(); }

    public Subscription subscribe(UUID subscriberId, Subscription.TargetType type, UUID targetId) {
        user(subscriberId);
        if (type == Subscription.TargetType.USER) user(targetId);
        else if (!groups.existsById(targetId)) throw new NoSuchElementException("Group not found: " + targetId);
        return subscriptions.save(new Subscription(UUID.randomUUID(), subscriberId, type, targetId));
    }

    @Transactional(readOnly = true)
    public List<User> upcomingForSubscriber(UUID subscriberId, int days) {
        var targetUsers = new HashSet<UUID>();
        for (var subscription : subscriptions.findBySubscriberId(subscriberId)) {
            if (subscription.getTargetType() == Subscription.TargetType.USER) {
                targetUsers.add(subscription.getTargetId());
            } else {
                memberships.findByGroupId(subscription.getTargetId()).stream()
                        .map(GroupMembership::getUserId).forEach(targetUsers::add);
            }
        }
        var today = LocalDate.now();
        return users.findAllById(targetUsers).stream()
                .filter(u -> daysUntil(u.getBirthDate(), today) <= days)
                .sorted(Comparator.comparingInt(u -> daysUntil(u.getBirthDate(), today)))
                .toList();
    }

    @Transactional(readOnly = true)
    public FriendCard card(UUID id) {
        var user = user(id);
        var groupList = memberships.findByUserId(id).stream()
                .map(GroupMembership::getGroupId).map(groups::findById)
                .flatMap(Optional::stream).map(FriendGroup::getName).toList();
        return new FriendCard(user.getId(), user.getName(), user.getBirthDate(), groupList,
                giftClient.wishes(id), "/api/chat/users/" + id + "/messages");
    }

    public static int daysUntil(LocalDate birthDate, LocalDate today) {
        var birthday = safeDate(today.getYear(), birthDate);
        if (birthday.isBefore(today)) birthday = safeDate(today.getYear() + 1, birthDate);
        return (int) Duration.between(today.atStartOfDay(), birthday.atStartOfDay()).toDays();
    }

    private static LocalDate safeDate(int year, LocalDate birthDate) {
        if (birthDate.getMonth() == Month.FEBRUARY && birthDate.getDayOfMonth() == 29 && !Year.isLeap(year)) {
            return LocalDate.of(year, Month.FEBRUARY, 28);
        }
        return birthDate.withYear(year);
    }

    public record FriendCard(UUID id, String name, LocalDate birthDate, List<String> groups,
                             List<GiftClient.Wish> wishes, String chatUrl) {}
}
