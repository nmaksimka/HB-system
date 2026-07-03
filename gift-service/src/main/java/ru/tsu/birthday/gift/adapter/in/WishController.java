package ru.tsu.birthday.gift.adapter.in;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import ru.tsu.birthday.gift.application.WishService;
import ru.tsu.birthday.gift.domain.Wish;
import java.util.*;

@RestController
public class WishController {
    private final WishService service;
    public WishController(WishService service) { this.service = service; }

    @PostMapping("/api/users/{userId}/wishes")
    @ResponseStatus(HttpStatus.CREATED)
    WishView add(@PathVariable UUID userId, @Valid @RequestBody CreateWish request) {
        return WishView.from(service.add(userId, request.title(), request.description(), request.link(), request.priority()));
    }

    @GetMapping({"/api/users/{userId}/wishes", "/internal/users/{userId}/wishes"})
    List<WishView> wishes(@PathVariable UUID userId) {
        return service.byUser(userId).stream().map(WishView::from).toList();
    }

    @DeleteMapping("/api/users/{userId}/wishes/{wishId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void delete(@PathVariable UUID userId, @PathVariable UUID wishId) { service.delete(userId, wishId); }

    record CreateWish(@NotBlank @Size(max = 160) String title, @Size(max = 1000) String description,
                      @Size(max = 500) String link, @Min(1) @Max(10) int priority) {}
    record WishView(UUID id, String title, String description, String link, Integer priority) {
        static WishView from(Wish w) {
            return new WishView(w.getId(), w.getTitle(), w.getDescription(), w.getLink(), w.getPriority());
        }
    }
}
