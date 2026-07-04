package ru.tsu.birthday.identity.controller;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import ru.tsu.birthday.identity.service.IdentityService;
import ru.tsu.birthday.identity.dto.*;
import ru.tsu.birthday.identity.entity.Subscription;
import ru.tsu.birthday.identity.mapper.IdentityMapper;
import ru.tsu.birthday.identity.security.CurrentUser;
import java.util.*;

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

    @PostMapping("/users")
    @ResponseStatus(HttpStatus.CREATED)
    UserResponse create(@Valid @RequestBody CreateUserRequest request) {
        return mapper.toResponse(service.createUser(request.name(), request.birthDate()));
    }

    @GetMapping("/users")
    List<UserResponse> users() { return service.users().stream().map(mapper::toResponse).toList(); }

    @GetMapping("/me")
    UserResponse me(@AuthenticationPrincipal Jwt jwt) { return mapper.toResponse(service.user(currentUser.id(jwt))); }

    @PostMapping("/me/profile")
    @ResponseStatus(HttpStatus.CREATED)
    UserResponse createProfile(@AuthenticationPrincipal Jwt jwt, @Valid @RequestBody CreateUserRequest request) {
        return mapper.toResponse(service.createProfile(currentUser.id(jwt), request.name(), request.birthDate()));
    }

    @GetMapping("/users/{id}")
    UserResponse user(@PathVariable UUID id) { return mapper.toResponse(service.user(id)); }

    @GetMapping("/users/{id}/card")
    FriendCardResponse card(@PathVariable UUID id) { return service.card(id); }

    @PostMapping("/groups")
    @ResponseStatus(HttpStatus.CREATED)
    GroupResponse createGroup(@AuthenticationPrincipal Jwt jwt, @Valid @RequestBody CreateGroupRequest request) {
        return mapper.toResponse(service.createGroup(request.name(), currentUser.id(jwt)));
    }

    @GetMapping("/groups")
    List<GroupResponse> groups() { return service.groups().stream().map(mapper::toResponse).toList(); }

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
        return mapper.toResponse(service.subscribeGroupAndMembers(currentUser.id(jwt), groupId));
    }

    @DeleteMapping("/subscriptions/users/{targetUserId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void unsubscribeUser(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID targetUserId) {
        service.unsubscribe(currentUser.id(jwt), Subscription.TargetType.USER, targetUserId);
    }

    @DeleteMapping("/subscriptions/groups/{groupId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void unsubscribeGroup(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID groupId) {
        service.unsubscribeGroupAndMembers(currentUser.id(jwt), groupId);
    }

    @GetMapping("/subscriptions")
    List<SubscriptionResponse> subscriptions(@AuthenticationPrincipal Jwt jwt) {
        return service.subscriptions(currentUser.id(jwt)).stream().map(mapper::toResponse).toList();
    }
}

@RestController
@RequestMapping("/internal")
class InternalIdentityController {
    private final IdentityService service;
    private final IdentityMapper mapper;

    InternalIdentityController(IdentityService service, IdentityMapper mapper) {
        this.service = service;
        this.mapper = mapper;
    }

    @GetMapping("/users/{id}/exists")
    boolean exists(@PathVariable UUID id) { return service.userExists(id); }

    @GetMapping("/users/{id}")
    UserResponse internalUser(@PathVariable UUID id) { return mapper.toResponse(service.user(id)); }

    @GetMapping("/subscribers/{subscriberId}/upcoming")
    List<UserResponse> upcoming(@PathVariable UUID subscriberId,
                            @RequestParam(defaultValue = "14") @Min(1) @Max(365) int days) {
        return service.upcomingForSubscriber(subscriberId, days).stream().map(mapper::toResponse).toList();
    }
}
