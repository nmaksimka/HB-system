package ru.tsu.birthday.identity.controller;

import jakarta.validation.Valid;
import java.util.*;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import ru.tsu.birthday.identity.dto.*;
import ru.tsu.birthday.identity.entity.Subscription;
import ru.tsu.birthday.identity.mapper.IdentityMapper;
import ru.tsu.birthday.identity.security.CurrentUser;
import ru.tsu.birthday.identity.service.IdentityService;

@RestController
@RequestMapping("/api")
public class IdentityController {
    private final IdentityService service;
    private final IdentityMapper mapper;
    private final CurrentUser currentUser;

    public IdentityController(IdentityService service, IdentityMapper mapper, CurrentUser currentUser) {
        this.service = service;
        this.mapper = mapper;
        this.currentUser = currentUser;
    }

    @GetMapping("/me")
    UserResponse me(@AuthenticationPrincipal Jwt jwt) {
        return mapper.toResponse(service.user(currentUser.id(jwt)));
    }

    @PostMapping("/me/profile")
    @ResponseStatus(HttpStatus.CREATED)
    UserResponse createProfile(@AuthenticationPrincipal Jwt jwt, @Valid @RequestBody CreateUserRequest request) {
        return mapper.toResponse(service.createProfile(currentUser.id(jwt), request.name(), request.birthDate()));
    }

    @GetMapping("/users")
    List<UserResponse> users() {
        return service.users().stream().map(mapper::toResponse).toList();
    }

    @GetMapping("/users/{id}/card")
    FriendCardResponse card(@PathVariable UUID id) {
        var user = service.user(id);
        return new FriendCardResponse(user.getId(), user.getName(), user.getBirthDate(), service.groupNames(id));
    }

    @PostMapping("/groups")
    @ResponseStatus(HttpStatus.CREATED)
    GroupResponse createGroup(@AuthenticationPrincipal Jwt jwt, @Valid @RequestBody CreateGroupRequest request) {
        var ownerId = currentUser.id(jwt);
        return mapper.toResponse(service.createGroup(request.name(), ownerId), List.of(ownerId));
    }

    @GetMapping("/groups")
    List<GroupResponse> groups() {
        var members = service.membersByGroup();
        return service.groups().stream()
                .map(group -> mapper.toResponse(group, members.getOrDefault(group.getId(), List.of())))
                .toList();
    }

    @GetMapping("/groups/{groupId}/members")
    List<UserResponse> members(@PathVariable UUID groupId) {
        return service.members(groupId).stream().map(mapper::toResponse).toList();
    }

    @PutMapping("/groups/{groupId}/members/me")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void join(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID groupId) {
        service.join(groupId, currentUser.id(jwt));
    }

    @DeleteMapping("/groups/{groupId}/members/me")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void leave(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID groupId) {
        service.leave(groupId, currentUser.id(jwt));
    }

    @PostMapping("/subscriptions/users/{targetUserId}")
    @ResponseStatus(HttpStatus.CREATED)
    SubscriptionResponse subscribeUser(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID targetUserId) {
        return mapper.toResponse(service.subscribe(currentUser.id(jwt), Subscription.TargetType.USER, targetUserId));
    }

    @PostMapping("/subscriptions/groups/{groupId}")
    @ResponseStatus(HttpStatus.CREATED)
    SubscriptionResponse subscribeGroup(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID groupId) {
        return mapper.toResponse(service.subscribe(currentUser.id(jwt), Subscription.TargetType.GROUP, groupId));
    }

    @DeleteMapping("/subscriptions/users/{targetUserId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void unsubscribeUser(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID targetUserId) {
        service.unsubscribe(currentUser.id(jwt), Subscription.TargetType.USER, targetUserId);
    }

    @DeleteMapping("/subscriptions/groups/{groupId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void unsubscribeGroup(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID groupId) {
        service.unsubscribe(currentUser.id(jwt), Subscription.TargetType.GROUP, groupId);
    }

    @GetMapping("/subscriptions")
    List<SubscriptionResponse> subscriptions(@AuthenticationPrincipal Jwt jwt) {
        return service.subscriptions(currentUser.id(jwt)).stream().map(mapper::toResponse).toList();
    }

    @GetMapping("/me/upcoming")
    List<UpcomingBirthdayResponse> upcoming(@AuthenticationPrincipal Jwt jwt) {
        return service.upcomingForSubscriber(currentUser.id(jwt), 366);
    }
}
