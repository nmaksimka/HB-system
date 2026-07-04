package ru.tsu.birthday.notification.client;

import java.util.List;
import java.util.UUID;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(name = "identityService", url = "${clients.identity.url}")
public interface IdentityClient {
    @GetMapping("/internal/subscribers")
    List<UUID> subscribers();

    @GetMapping("/internal/subscribers/{subscriberId}/upcoming")
    List<UpcomingBirthday> upcoming(@PathVariable UUID subscriberId, @RequestParam int days);
}
