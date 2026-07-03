package ru.tsu.birthday.notification.adapter.out;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDate;
import java.util.*;

@FeignClient(name = "identity-service", url = "${clients.identity.url}", fallback = IdentityClient.Fallback.class)
public interface IdentityClient {
    @GetMapping("/api/internal/subscribers/{subscriberId}/upcoming")
    List<UserView> upcoming(@PathVariable UUID subscriberId, @RequestParam int days);

    record UserView(UUID id, String name, LocalDate birthDate) {}

    @Component
    class Fallback implements IdentityClient {
        @Override public List<UserView> upcoming(UUID subscriberId, int days) { return List.of(); }
    }
}
