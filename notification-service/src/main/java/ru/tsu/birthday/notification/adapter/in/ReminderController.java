package ru.tsu.birthday.notification.adapter.in;

import jakarta.validation.constraints.*;
import org.springframework.web.bind.annotation.*;
import ru.tsu.birthday.notification.application.ReminderService;
import ru.tsu.birthday.notification.domain.Reminder;
import java.time.*;
import java.util.*;

@RestController
@RequestMapping("/api/notifications")
public class ReminderController {
    private final ReminderService service;
    public ReminderController(ReminderService service) { this.service = service; }

    @PostMapping("/users/{userId}/refresh")
    List<ReminderView> refresh(@PathVariable UUID userId,
                               @RequestParam(defaultValue = "14") @Min(1) @Max(365) int days) {
        return service.refresh(userId, days).stream().map(ReminderView::from).toList();
    }

    @PutMapping("/users/{userId}/{reminderId}/read")
    ReminderView read(@PathVariable UUID userId, @PathVariable UUID reminderId) {
        return ReminderView.from(service.markRead(userId, reminderId));
    }

    record ReminderView(UUID id, UUID birthdayUserId, String birthdayUserName, LocalDate birthdayDate,
                        Instant createdAt, Instant readAt) {
        static ReminderView from(Reminder r) {
            return new ReminderView(r.getId(), r.getBirthdayUserId(), r.getBirthdayUserName(),
                    r.getBirthdayDate(), r.getCreatedAt(), r.getReadAt());
        }
    }
}
