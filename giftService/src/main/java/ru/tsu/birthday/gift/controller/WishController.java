package ru.tsu.birthday.gift.controller;

import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import ru.tsu.birthday.gift.dto.CreateWishRequest;
import ru.tsu.birthday.gift.dto.WishResponse;
import ru.tsu.birthday.gift.dto.WishSummary;
import ru.tsu.birthday.gift.mapper.WishMapper;
import ru.tsu.birthday.gift.security.CurrentUser;
import ru.tsu.birthday.gift.service.WishService;

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
        var userId = currentUser.id(jwt);
        var wish = service.add(userId, request.title(), request.description(), request.link(), request.priority());
        return mapper.toResponse(wish, userId);
    }

    @GetMapping("/api/me/wishes")
    List<WishResponse> myWishes(@AuthenticationPrincipal Jwt jwt) {
        return wishes(jwt, currentUser.id(jwt));
    }

    @GetMapping("/api/users/{userId}/wishes")
    List<WishResponse> wishes(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID userId) {
        var viewerId = currentUser.id(jwt);
        return service.byUser(userId).stream().map(wish -> mapper.toResponse(wish, viewerId)).toList();
    }

    @DeleteMapping("/api/me/wishes/{wishId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void delete(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID wishId) {
        service.delete(currentUser.id(jwt), wishId);
    }

    @PutMapping("/api/users/{userId}/wishes/{wishId}/reservation")
    WishResponse reserve(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID userId, @PathVariable UUID wishId) {
        var viewerId = currentUser.id(jwt);
        return mapper.toResponse(service.reserve(viewerId, userId, wishId), viewerId);
    }

    @DeleteMapping("/api/users/{userId}/wishes/{wishId}/reservation")
    WishResponse cancelReservation(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID userId,
                                   @PathVariable UUID wishId) {
        var viewerId = currentUser.id(jwt);
        return mapper.toResponse(service.cancelReservation(viewerId, userId, wishId), viewerId);
    }

    @GetMapping("/internal/wishes/{wishId}")
    WishSummary summary(@PathVariable UUID wishId) {
        var wish = service.wish(wishId);
        return new WishSummary(wish.getId(), wish.getUserId(), wish.getTitle());
    }
}
