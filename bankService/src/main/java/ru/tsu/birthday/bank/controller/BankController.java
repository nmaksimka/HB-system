package ru.tsu.birthday.bank.controller;

import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import ru.tsu.birthday.bank.dto.CollectionResponse;
import ru.tsu.birthday.bank.dto.OpenCollectionRequest;
import ru.tsu.birthday.bank.dto.PayRequest;
import ru.tsu.birthday.bank.dto.PaymentResponse;
import ru.tsu.birthday.bank.security.CurrentUser;
import ru.tsu.birthday.bank.service.BankService;

@RestController
public class BankController {
    private final BankService service;
    private final CurrentUser currentUser;

    public BankController(BankService service, CurrentUser currentUser) {
        this.service = service;
        this.currentUser = currentUser;
    }

    @PostMapping("/internal/collections")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void open(@Valid @RequestBody OpenCollectionRequest request) {
        service.open(request);
    }

    @GetMapping("/api/bank/collections/{fundraiserId}")
    CollectionResponse collection(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID fundraiserId) {
        return service.collection(currentUser.id(jwt), fundraiserId);
    }

    @PostMapping("/api/bank/collections/{fundraiserId}/payments")
    @ResponseStatus(HttpStatus.CREATED)
    PaymentResponse pay(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID fundraiserId,
                        @Valid @RequestBody PayRequest request) {
        var payment = service.pay(currentUser.id(jwt), fundraiserId, request);
        return new PaymentResponse(payment.getId(), payment.getFundraiserId(), payment.getPayerId(),
                payment.getAmount(), payment.getPaidAt());
    }
}
