package ru.tsu.birthday.identity.adapter.in;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import ru.tsu.birthday.identity.application.IdentityFacade;
import ru.tsu.birthday.identity.domain.*;
import java.time.LocalDate;
import java.util.*;

@RestController
@RequestMapping("/api")
public class IdentityController {
    private final IdentityFacade facade;

    public IdentityController(IdentityFacade facade) { this.facade = facade; }

    @PostMapping("/users")
    @ResponseStatus(HttpStatus.CREATED)
    UserView create(@Valid @RequestBody CreateUser request) {
        return UserView.from(facade.createUser(request.name(), request.birthDate()));
    }

    @GetMapping("/users")
    List<UserView> users() { return facade.users().stream().map(UserView::from).toList(); }

    @GetMapping("/users/{id}")
    UserView user(@PathVariable UUID id) { return UserView.from(facade.user(id)); }

    @GetMapping("/users/{id}/card")
    IdentityFacade.FriendCard card(@PathVariable UUID id) { return facade.card(id); }

    @PostMapping("/groups")
    @ResponseStatus(HttpStatus.CREATED)
    GroupView createGroup(@Valid @RequestBody CreateGroup request) {
        return GroupView.from(facade.createGroup(request.name(), request.ownerId()));
    }

    @GetMapping("/groups")
    List<GroupView> groups() { return facade.groups().stream().map(GroupView::from).toList(); }

    @PutMapping("/groups/{groupId}/members/{userId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void join(@PathVariable UUID groupId, @PathVariable UUID userId) { facade.join(groupId, userId); }

    @PostMapping("/subscriptions")
    @ResponseStatus(HttpStatus.CREATED)
    SubscriptionView subscribe(@Valid @RequestBody Subscribe request) {
        var s = facade.subscribe(request.subscriberId(), request.targetType(), request.targetId());
        return new SubscriptionView(s.getSubscriberId(), s.getTargetType(), s.getTargetId());
    }

    @GetMapping("/internal/subscribers/{subscriberId}/upcoming")
    List<UserView> upcoming(@PathVariable UUID subscriberId,
                            @RequestParam(defaultValue = "14") @Min(1) @Max(365) int days) {
        return facade.upcomingForSubscriber(subscriberId, days).stream().map(UserView::from).toList();
    }

    record CreateUser(@NotBlank @Size(max = 120) String name, @Past LocalDate birthDate) {}
    record CreateGroup(@NotBlank @Size(max = 160) String name, @NotNull UUID ownerId) {}
    record Subscribe(@NotNull UUID subscriberId, @NotNull Subscription.TargetType targetType,
                     @NotNull UUID targetId) {}
    record UserView(UUID id, String name, LocalDate birthDate) {
        static UserView from(User user) { return new UserView(user.getId(), user.getName(), user.getBirthDate()); }
    }
    record GroupView(UUID id, String name, UUID ownerId) {
        static GroupView from(FriendGroup group) {
            return new GroupView(group.getId(), group.getName(), group.getOwnerId());
        }
    }
    record SubscriptionView(UUID subscriberId, Subscription.TargetType targetType, UUID targetId) {}
}
