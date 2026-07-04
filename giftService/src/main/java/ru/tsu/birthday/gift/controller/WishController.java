package ru.tsu.birthday.gift.controller;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import ru.tsu.birthday.gift.service.WishService;
import ru.tsu.birthday.gift.dto.*;
import ru.tsu.birthday.gift.mapper.WishMapper;
import ru.tsu.birthday.gift.security.CurrentUser;
import java.util.*;

@RestController
public class WishController {
    private final WishService service;
    private final WishMapper mapper;
    private final CurrentUser currentUser;

    public WishController(WishService service, WishMapper mapper, CurrentUser currentUser) {
        this.service = service;
        this.mapper = mapper;
        this.currentUser = currentUser;
    }

    @PostMapping("/api/me/wishes")
    @ResponseStatus(HttpStatus.CREATED)
    WishResponse add(@AuthenticationPrincipal Jwt jwt, @Valid @RequestBody CreateWishRequest request) {
        return mapper.toResponse(service.add(
                currentUser.id(jwt), request.title(), request.description(), request.link(), request.priority()));
    }

    @GetMapping({"/api/users/{userId}/wishes", "/internal/users/{userId}/wishes"})
    List<WishResponse> wishes(@PathVariable UUID userId) {
        return service.byUser(userId).stream().map(mapper::toResponse).toList();
    }

    @GetMapping("/api/me/wishes")
    List<WishResponse> myWishes(@AuthenticationPrincipal Jwt jwt) {
        return service.byUser(currentUser.id(jwt)).stream().map(mapper::toResponse).toList();
    }

    @DeleteMapping("/api/me/wishes/{wishId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void delete(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID wishId) {
        service.delete(currentUser.id(jwt), wishId);
    }

    @PutMapping("/api/users/{userId}/wishes/{wishId}/reservation")
    WishResponse reserve(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID userId, @PathVariable UUID wishId) {
        return mapper.toResponse(service.reserve(currentUser.id(jwt), userId, wishId));
    }

    @DeleteMapping("/api/users/{userId}/wishes/{wishId}/reservation")
    WishResponse cancelReservation(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID userId,
                                   @PathVariable UUID wishId) {
        return mapper.toResponse(service.cancelReservation(currentUser.id(jwt), userId, wishId));
    }
}
