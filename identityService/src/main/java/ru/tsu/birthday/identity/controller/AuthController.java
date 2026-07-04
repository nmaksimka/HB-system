package ru.tsu.birthday.identity.controller;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import ru.tsu.birthday.identity.dto.RegisterRequest;
import ru.tsu.birthday.identity.dto.UserResponse;
import ru.tsu.birthday.identity.mapper.IdentityMapper;
import ru.tsu.birthday.identity.service.RegistrationService;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private final RegistrationService registrationService;
    private final IdentityMapper mapper;

    public AuthController(RegistrationService registrationService, IdentityMapper mapper) {
        this.registrationService = registrationService;
        this.mapper = mapper;
    }

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    UserResponse register(@Valid @RequestBody RegisterRequest request) {
        return mapper.toResponse(registrationService.register(request));
    }
}
