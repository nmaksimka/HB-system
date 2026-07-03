package ru.tsu.birthday.gift.controller;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import ru.tsu.birthday.gift.service.WishService;
import ru.tsu.birthday.gift.dto.*;
import ru.tsu.birthday.gift.mapper.WishMapper;
import java.util.*;

@RestController
public class WishController {
    private final WishService service;
    private final WishMapper mapper;

    public WishController(WishService service, WishMapper mapper) {
        this.service = service;
        this.mapper = mapper;
    }

    @PostMapping("/api/users/{userId}/wishes")
    @ResponseStatus(HttpStatus.CREATED)
    WishResponse add(@PathVariable UUID userId, @Valid @RequestBody CreateWishRequest request) {
        return mapper.toResponse(service.add(
                userId, request.title(), request.description(), request.link(), request.priority()));
    }

    @GetMapping({"/api/users/{userId}/wishes", "/internal/users/{userId}/wishes"})
    List<WishResponse> wishes(@PathVariable UUID userId) {
        return service.byUser(userId).stream().map(mapper::toResponse).toList();
    }

    @DeleteMapping("/api/users/{userId}/wishes/{wishId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void delete(@PathVariable UUID userId, @PathVariable UUID wishId) { service.delete(userId, wishId); }
}
