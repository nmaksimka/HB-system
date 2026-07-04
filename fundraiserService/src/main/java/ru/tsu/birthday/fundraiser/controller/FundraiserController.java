package ru.tsu.birthday.fundraiser.controller;

import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import ru.tsu.birthday.fundraiser.dto.ContributionResponse;
import ru.tsu.birthday.fundraiser.dto.CreateFundraiserRequest;
import ru.tsu.birthday.fundraiser.dto.FundraiserResponse;
import ru.tsu.birthday.fundraiser.entity.Fundraiser;
import ru.tsu.birthday.fundraiser.security.CurrentUser;
import ru.tsu.birthday.fundraiser.service.FundraiserService;

@RestController
@RequestMapping("/api")
public class FundraiserController {
    private final FundraiserService service;
    private final CurrentUser currentUser;

    public FundraiserController(FundraiserService service, CurrentUser currentUser) {
        this.service = service;
        this.currentUser = currentUser;
    }

    @PostMapping("/fundraisers")
    @ResponseStatus(HttpStatus.CREATED)
    FundraiserResponse create(@AuthenticationPrincipal Jwt jwt, @Valid @RequestBody CreateFundraiserRequest request) {
        return toResponse(service.create(currentUser.id(jwt), request), List.of());
    }

    @GetMapping("/users/{beneficiaryId}/fundraisers")
    List<FundraiserResponse> forBeneficiary(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID beneficiaryId) {
        return service.forBeneficiary(currentUser.id(jwt), beneficiaryId).stream()
                .map(fundraiser -> toResponse(fundraiser, List.of()))
                .toList();
    }

    @GetMapping("/fundraisers/{fundraiserId}")
    FundraiserResponse get(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID fundraiserId) {
        var fundraiser = service.get(currentUser.id(jwt), fundraiserId);
        var contributions = service.contributions(fundraiserId).stream()
                .map(c -> new ContributionResponse(c.getPayerId(), c.getAmount(), c.getPaidAt()))
                .toList();
        return toResponse(fundraiser, contributions);
    }

    @PostMapping("/fundraisers/{fundraiserId}/cancel")
    FundraiserResponse cancel(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID fundraiserId) {
        return toResponse(service.cancel(currentUser.id(jwt), fundraiserId), List.of());
    }

    private FundraiserResponse toResponse(Fundraiser fundraiser, List<ContributionResponse> contributions) {
        return new FundraiserResponse(fundraiser.getId(), fundraiser.getOwnerId(), fundraiser.getBeneficiaryId(),
                fundraiser.getWishId(), fundraiser.getTitle(), fundraiser.getTargetAmount(),
                fundraiser.getCollectedAmount(), fundraiser.getStatus(), fundraiser.getCreatedAt(),
                fundraiser.getClosedAt(), contributions);
    }
}
