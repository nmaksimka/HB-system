package ru.tsu.birthday.notification.controller;

import jakarta.validation.constraints.*;
import org.springframework.web.bind.annotation.*;
import ru.tsu.birthday.notification.service.ReminderService;
import ru.tsu.birthday.notification.dto.ReminderResponse;
import ru.tsu.birthday.notification.mapper.ReminderMapper;
import java.time.*;
import java.util.*;

@RestController
@RequestMapping("/api/notifications")
public class ReminderController {
    private final ReminderService service;
    private final ReminderMapper mapper;

    public ReminderController(ReminderService service, ReminderMapper mapper) {
        this.service = service;
        this.mapper = mapper;
    }

    @PostMapping("/users/{userId}/refresh")
    List<ReminderResponse> refresh(@PathVariable UUID userId,
                               @RequestParam(defaultValue = "14") @Min(1) @Max(365) int days) {
        return service.refresh(userId, days).stream().map(mapper::toResponse).toList();
    }

    @PutMapping("/users/{userId}/{reminderId}/read")
    ReminderResponse read(@PathVariable UUID userId, @PathVariable UUID reminderId) {
        return mapper.toResponse(service.markRead(userId, reminderId));
    }
}
