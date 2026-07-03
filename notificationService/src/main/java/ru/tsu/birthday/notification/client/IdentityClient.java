package ru.tsu.birthday.notification.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;
import java.util.*;

@FeignClient(name = "identityService", url = "${clients.identity.url}")
public interface IdentityClient {
    @GetMapping("/api/internal/subscribers/{subscriberId}/upcoming")
    List<UpcomingUserResponse> upcoming(@PathVariable UUID subscriberId, @RequestParam int days);
}
