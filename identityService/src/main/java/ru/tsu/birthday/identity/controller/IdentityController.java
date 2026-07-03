package ru.tsu.birthday.identity.controller;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import ru.tsu.birthday.identity.service.IdentityService;
import ru.tsu.birthday.identity.dto.*;
import ru.tsu.birthday.identity.mapper.IdentityMapper;
import java.time.LocalDate;
import java.util.*;

@RestController
@RequestMapping("/api")
public class IdentityController {
    private final IdentityService service;
    private final IdentityMapper mapper;

    public IdentityController(IdentityService service, IdentityMapper mapper) {
        this.service = service;
        this.mapper = mapper;
    }

    @PostMapping("/users")
    @ResponseStatus(HttpStatus.CREATED)
    UserResponse create(@Valid @RequestBody CreateUserRequest request) {
        return mapper.toResponse(service.createUser(request.name(), request.birthDate()));
    }

    @GetMapping("/users")
    List<UserResponse> users() { return service.users().stream().map(mapper::toResponse).toList(); }

    @GetMapping("/users/{id}")
    UserResponse user(@PathVariable UUID id) { return mapper.toResponse(service.user(id)); }

    @GetMapping("/users/{id}/card")
    FriendCardResponse card(@PathVariable UUID id) { return service.card(id); }

    @PostMapping("/groups")
    @ResponseStatus(HttpStatus.CREATED)
    GroupResponse createGroup(@Valid @RequestBody CreateGroupRequest request) {
        return mapper.toResponse(service.createGroup(request.name(), request.ownerId()));
    }

    @GetMapping("/groups")
    List<GroupResponse> groups() { return service.groups().stream().map(mapper::toResponse).toList(); }

    @PutMapping("/groups/{groupId}/members/{userId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void join(@PathVariable UUID groupId, @PathVariable UUID userId) { service.join(groupId, userId); }

    @PostMapping("/subscriptions")
    @ResponseStatus(HttpStatus.CREATED)
    SubscriptionResponse subscribe(@Valid @RequestBody CreateSubscriptionRequest request) {
        var s = service.subscribe(request.subscriberId(), request.targetType(), request.targetId());
        return mapper.toResponse(s);
    }

    @GetMapping("/internal/subscribers/{subscriberId}/upcoming")
    List<UserResponse> upcoming(@PathVariable UUID subscriberId,
                            @RequestParam(defaultValue = "14") @Min(1) @Max(365) int days) {
        return service.upcomingForSubscriber(subscriberId, days).stream().map(mapper::toResponse).toList();
    }
}
