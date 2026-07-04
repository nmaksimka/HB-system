package ru.tsu.birthday.identity.service;

import java.time.Clock;
import java.time.LocalDate;
import java.util.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.tsu.birthday.identity.dto.UpcomingBirthdayResponse;
import ru.tsu.birthday.identity.entity.*;
import ru.tsu.birthday.identity.repository.*;

@Service
@Transactional
public class IdentityService {
    private final UserRepository users;
    private final GroupRepository groups;
    private final MembershipRepository memberships;
    private final SubscriptionRepository subscriptions;
    private final Clock clock;

    public IdentityService(UserRepository users, GroupRepository groups, MembershipRepository memberships,
                           SubscriptionRepository subscriptions, Clock clock) {
        this.users = users;
        this.groups = groups;
        this.memberships = memberships;
        this.subscriptions = subscriptions;
        this.clock = clock;
    }

    public User createProfile(UUID id, String name, LocalDate birthDate) {
        return users.findById(id).orElseGet(() -> users.save(new User(id, name.trim(), birthDate)));
    }

    @Transactional(readOnly = true)
    public List<User> users() {
        return users.findAll().stream().sorted(Comparator.comparing(User::getName)).toList();
    }

    @Transactional(readOnly = true)
    public User user(UUID id) {
        return users.findById(id).orElseThrow(() -> new NoSuchElementException("User not found: " + id));
    }

    @Transactional(readOnly = true)
    public boolean userExists(UUID id) {
        return users.existsById(id);
    }

    @Transactional(readOnly = true)
    public List<String> groupNames(UUID userId) {
        var groupIds = memberships.findByUserId(userId).stream().map(GroupMembership::getGroupId).toList();
        return groups.findAllById(groupIds).stream().map(FriendGroup::getName).sorted().toList();
    }

    public FriendGroup createGroup(String name, UUID ownerId) {
        user(ownerId);
        var trimmed = name.trim();
        if (groups.existsByNameIgnoreCase(trimmed)) {
            throw new IllegalStateException("Group with this name already exists");
        }
        var group = groups.save(new FriendGroup(UUID.randomUUID(), trimmed, ownerId));
        memberships.save(new GroupMembership(UUID.randomUUID(), group.getId(), ownerId));
        return group;
    }

    @Transactional(readOnly = true)
    public List<FriendGroup> groups() {
        return groups.findAll().stream().sorted(Comparator.comparing(FriendGroup::getName)).toList();
    }

    @Transactional(readOnly = true)
    public Map<UUID, List<UUID>> membersByGroup() {
        var result = new HashMap<UUID, List<UUID>>();
        for (var membership : memberships.findAll()) {
            result.computeIfAbsent(membership.getGroupId(), id -> new ArrayList<>()).add(membership.getUserId());
        }
        return result;
    }

    @Transactional(readOnly = true)
    public List<User> members(UUID groupId) {
        requireGroup(groupId);
        return users.findAllById(memberIds(groupId));
    }

    public void join(UUID groupId, UUID userId) {
        requireGroup(groupId);
        user(userId);
        if (!memberships.existsByGroupIdAndUserId(groupId, userId)) {
            memberships.save(new GroupMembership(UUID.randomUUID(), groupId, userId));
        }
    }

    public void leave(UUID groupId, UUID userId) {
        requireGroup(groupId);
        memberships.deleteByGroupIdAndUserId(groupId, userId);
    }

    public Subscription subscribe(UUID subscriberId, Subscription.TargetType type, UUID targetId) {
        user(subscriberId);
        if (type == Subscription.TargetType.USER) {
            if (subscriberId.equals(targetId)) {
                throw new IllegalArgumentException("You cannot subscribe to yourself");
            }
            user(targetId);
        } else {
            requireGroup(targetId);
        }
        return subscriptions.findBySubscriberIdAndTargetTypeAndTargetId(subscriberId, type, targetId)
                .orElseGet(() -> subscriptions.save(new Subscription(UUID.randomUUID(), subscriberId, type, targetId)));
    }

    public void unsubscribe(UUID subscriberId, Subscription.TargetType type, UUID targetId) {
        subscriptions.deleteBySubscriberIdAndTargetTypeAndTargetId(subscriberId, type, targetId);
    }

    @Transactional(readOnly = true)
    public List<Subscription> subscriptions(UUID subscriberId) {
        return subscriptions.findBySubscriberId(subscriberId);
    }

    @Transactional(readOnly = true)
    public List<UUID> subscriberIds() {
        return subscriptions.findDistinctSubscriberIds();
    }

    @Transactional(readOnly = true)
    public List<UpcomingBirthdayResponse> upcomingForSubscriber(UUID subscriberId, int days) {
        var targetIds = new HashSet<UUID>();
        for (var subscription : subscriptions.findBySubscriberId(subscriberId)) {
            if (subscription.getTargetType() == Subscription.TargetType.USER) {
                targetIds.add(subscription.getTargetId());
            } else {
                targetIds.addAll(memberIds(subscription.getTargetId()));
            }
        }
        targetIds.remove(subscriberId);
        var today = LocalDate.now(clock);
        return users.findAllById(targetIds).stream()
                .map(user -> new UpcomingBirthdayResponse(user.getId(), user.getName(), user.getBirthDate(),
                        Birthdays.next(user.getBirthDate(), today), Birthdays.daysUntil(user.getBirthDate(), today)))
                .filter(upcoming -> upcoming.daysLeft() <= days)
                .sorted(Comparator.comparingInt(UpcomingBirthdayResponse::daysLeft))
                .toList();
    }

    @Transactional(readOnly = true)
    public boolean canDiscuss(UUID subjectId, UUID viewerId) {
        if (subjectId.equals(viewerId) || !users.existsById(subjectId)) {
            return false;
        }
        if (subscriptions.findBySubscriberIdAndTargetTypeAndTargetId(
                viewerId, Subscription.TargetType.USER, subjectId).isPresent()) {
            return true;
        }
        var viewerGroups = memberships.findByUserId(viewerId).stream().map(GroupMembership::getGroupId).toList();
        return memberships.findByUserId(subjectId).stream().anyMatch(m -> viewerGroups.contains(m.getGroupId()));
    }

    private List<UUID> memberIds(UUID groupId) {
        return memberships.findByGroupId(groupId).stream().map(GroupMembership::getUserId).toList();
    }

    private void requireGroup(UUID groupId) {
        if (!groups.existsById(groupId)) {
            throw new NoSuchElementException("Group not found: " + groupId);
        }
    }
}
