package ru.tsu.birthday.identity.controller;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.util.List;
import java.util.UUID;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import ru.tsu.birthday.identity.dto.UpcomingBirthdayResponse;
import ru.tsu.birthday.identity.service.IdentityService;

@Validated
@RestController
@RequestMapping("/internal")
class InternalIdentityController {
    private final IdentityService service;

    InternalIdentityController(IdentityService service) {
        this.service = service;
    }

    @GetMapping("/users/{id}/exists")
    boolean exists(@PathVariable UUID id) {
        return service.userExists(id);
    }

    @GetMapping("/users/{subjectId}/discussions/{viewerId}")
    boolean canDiscuss(@PathVariable UUID subjectId, @PathVariable UUID viewerId) {
        return service.canDiscuss(subjectId, viewerId);
    }

    @GetMapping("/subscribers")
    List<UUID> subscribers() {
        return service.subscriberIds();
    }

    @GetMapping("/subscribers/{subscriberId}/upcoming")
    List<UpcomingBirthdayResponse> upcoming(@PathVariable UUID subscriberId,
                                            @RequestParam(defaultValue = "30") @Min(0) @Max(366) int days) {
        return service.upcomingForSubscriber(subscriberId, days);
    }
}
