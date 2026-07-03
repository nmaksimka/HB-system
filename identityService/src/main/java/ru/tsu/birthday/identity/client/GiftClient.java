package ru.tsu.birthday.identity.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import ru.tsu.birthday.identity.dto.WishResponse;
import java.util.*;

@FeignClient(name = "giftService", url = "${clients.gift.url}")
public interface GiftClient {
    @GetMapping("/internal/users/{userId}/wishes")
    List<WishResponse> wishes(@PathVariable UUID userId);
}
