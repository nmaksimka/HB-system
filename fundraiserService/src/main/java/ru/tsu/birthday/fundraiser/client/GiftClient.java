package ru.tsu.birthday.fundraiser.client;

import java.util.UUID;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "giftClient", url = "${clients.gift.url}")
public interface GiftClient {
    @GetMapping("/internal/wishes/{wishId}")
    WishSummary wish(@PathVariable UUID wishId);
}
