package ru.tsu.birthday.identity.service;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.web.client.RestClient;
import ru.tsu.birthday.identity.dto.RegisterRequest;
import ru.tsu.birthday.identity.entity.User;

@Service
public class RegistrationService {
    private final IdentityService identityService;
    private final RestClient keycloak;
    private final String realm;
    private final String adminUsername;
    private final String adminPassword;

    public RegistrationService(
            IdentityService identityService,
            @Value("${birthday.keycloak.base-url}") String keycloakBaseUrl,
            @Value("${birthday.keycloak.realm}") String realm,
            @Value("${birthday.keycloak.admin-username}") String adminUsername,
            @Value("${birthday.keycloak.admin-password}") String adminPassword
    ) {
        var requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(Duration.ofSeconds(2));
        requestFactory.setReadTimeout(Duration.ofSeconds(5));
        this.identityService = identityService;
        this.keycloak = RestClient.builder().baseUrl(keycloakBaseUrl).requestFactory(requestFactory).build();
        this.realm = realm;
        this.adminUsername = adminUsername;
        this.adminPassword = adminPassword;
    }

    public User register(RegisterRequest request) {
        var token = adminToken();
        var userId = createKeycloakUser(token, request);
        try {
            return identityService.createProfile(userId, request.name(), request.birthDate());
        } catch (RuntimeException ex) {
            deleteKeycloakUser(token, userId);
            throw ex;
        }
    }

    private UUID createKeycloakUser(String token, RegisterRequest request) {
        var username = request.username().trim().toLowerCase();
        var payload = Map.of(
                "username", username,
                "enabled", true,
                "firstName", request.name().trim(),
                "lastName", "-",
                "email", username + "@birthday-hub.local",
                "emailVerified", true,
                "requiredActions", List.of(),
                "credentials", List.of(Map.of("type", "password", "value", request.password(), "temporary", false))
        );

        var response = keycloak.post()
                .uri("/admin/realms/{realm}/users", realm)
                .header("Authorization", "Bearer " + token)
                .body(payload)
                .retrieve()
                .onStatus(HttpStatusCode::isError, (req, res) -> {
                    if (res.getStatusCode().value() == 409) {
                        throw new RegistrationException(HttpStatus.CONFLICT, "This login is already taken");
                    }
                    throw new RegistrationException(HttpStatus.BAD_GATEWAY,
                            "Authorization server rejected the user: " + res.getStatusCode());
                })
                .toBodilessEntity();

        var location = response.getHeaders().getLocation();
        if (location == null) {
            throw new RegistrationException(HttpStatus.BAD_GATEWAY, "Authorization server did not return user id");
        }
        var path = location.getPath();
        return UUID.fromString(path.substring(path.lastIndexOf('/') + 1));
    }

    private void deleteKeycloakUser(String token, UUID userId) {
        keycloak.delete()
                .uri("/admin/realms/{realm}/users/{id}", realm, userId)
                .header("Authorization", "Bearer " + token)
                .retrieve()
                .toBodilessEntity();
    }

    private String adminToken() {
        var form = new LinkedMultiValueMap<String, String>();
        form.add("grant_type", "password");
        form.add("client_id", "admin-cli");
        form.add("username", adminUsername);
        form.add("password", adminPassword);

        var response = keycloak.post()
                .uri("/realms/master/protocol/openid-connect/token")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(form)
                .retrieve()
                .body(TokenResponse.class);

        if (response == null || response.accessToken() == null) {
            throw new RegistrationException(HttpStatus.BAD_GATEWAY, "Authorization server returned empty token");
        }
        return response.accessToken();
    }

    private record TokenResponse(@JsonProperty("access_token") String accessToken) {}
}
