package ru.tsu.birthday.gift.client;

import java.util.UUID;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "identityClient", url = "${clients.identity.url}")
public interface IdentityClient {
    @GetMapping("/internal/users/{id}/exists")
    boolean exists(@PathVariable UUID id);
}
