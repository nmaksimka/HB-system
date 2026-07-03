package ru.tsu.birthday.identity.adapter.out;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import java.util.*;

@FeignClient(name = "gift-service", url = "${clients.gift.url}", fallback = GiftClient.Fallback.class)
public interface GiftClient {
    @GetMapping("/internal/users/{userId}/wishes")
    List<Wish> wishes(@PathVariable UUID userId);

    record Wish(UUID id, String title, String description, String link, Integer priority) {}

    @Component
    class Fallback implements GiftClient {
        @Override public List<Wish> wishes(UUID userId) { return List.of(); }
    }
}
