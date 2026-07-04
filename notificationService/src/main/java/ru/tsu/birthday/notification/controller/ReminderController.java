package ru.tsu.birthday.notification.controller;

import jakarta.validation.constraints.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import ru.tsu.birthday.notification.service.ReminderService;
import ru.tsu.birthday.notification.dto.ReminderResponse;
import ru.tsu.birthday.notification.mapper.ReminderMapper;
import ru.tsu.birthday.notification.security.CurrentUser;
import java.util.*;

@RestController
@RequestMapping("/api/notifications")
public class ReminderController {
    private final ReminderService service;
    private final ReminderMapper mapper;
    private final CurrentUser currentUser;

    public ReminderController(ReminderService service, ReminderMapper mapper, CurrentUser currentUser) {
        this.service = service;
        this.mapper = mapper;
        this.currentUser = currentUser;
    }

    @PostMapping("/refresh")
    List<ReminderResponse> refresh(@AuthenticationPrincipal Jwt jwt,
                               @RequestParam(defaultValue = "14") @Min(1) @Max(365) int days) {
        return service.refresh(currentUser.id(jwt), days).stream().map(mapper::toResponse).toList();
    }

    @GetMapping
    List<ReminderResponse> reminders(@AuthenticationPrincipal Jwt jwt) {
        return service.reminders(currentUser.id(jwt)).stream().map(mapper::toResponse).toList();
    }

    @GetMapping("/unread")
    List<ReminderResponse> unread(@AuthenticationPrincipal Jwt jwt) {
        return service.unread(currentUser.id(jwt)).stream().map(mapper::toResponse).toList();
    }

    @PutMapping("/{reminderId}/read")
    ReminderResponse read(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID reminderId) {
        return mapper.toResponse(service.markRead(currentUser.id(jwt), reminderId));
    }

    @PutMapping("/read-all")
    List<ReminderResponse> readAll(@AuthenticationPrincipal Jwt jwt) {
        return service.markAllRead(currentUser.id(jwt)).stream().map(mapper::toResponse).toList();
    }
}
