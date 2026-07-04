package ru.tsu.birthday.identity.service;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatusCode;
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
            @Value("${birthday.keycloak.base-url:http://localhost:8080}") String keycloakBaseUrl,
            @Value("${birthday.keycloak.realm:birthday-hub}") String realm,
            @Value("${birthday.keycloak.admin-username:admin}") String adminUsername,
            @Value("${birthday.keycloak.admin-password:admin}") String adminPassword
    ) {
        this.identityService = identityService;
        this.keycloak = RestClient.builder().baseUrl(keycloakBaseUrl).build();
        this.realm = realm;
        this.adminUsername = adminUsername;
        this.adminPassword = adminPassword;
    }

    public User register(RegisterRequest request) {
        var userId = createKeycloakUser(request);
        return identityService.createProfile(userId, request.username().trim(), request.birthDate());
    }

    private UUID createKeycloakUser(RegisterRequest request) {
        var token = adminToken();
        var username = request.username().trim();
        var email = username + "@birthday-hub.local";
        var payload = Map.of(
                "username", username,
                "enabled", true,
                "firstName", username,
                "lastName", "Birthday Hub",
                "email", email,
                "emailVerified", true,
                "requiredActions", List.of(),
                "credentials", List.of(Map.of(
                        "type", "password",
                        "value", request.password(),
                        "temporary", false
                ))
        );

        var responseEntity = keycloak.post()
                .uri("/admin/realms/{realm}/users", realm)
                .header("Authorization", "Bearer " + token)
                .body(payload)
                .retrieve()
                .onStatus(HttpStatusCode::isError, (requestSpec, response) -> {
                    if (response.getStatusCode().value() == 409) {
                        throw new IllegalArgumentException("Пользователь с таким логином уже существует");
                    }
                    throw new IllegalStateException("Keycloak не создал пользователя: " + response.getStatusCode());
                })
                .toBodilessEntity();

        var location = responseEntity.getHeaders().getLocation();
        if (location == null) {
            throw new IllegalStateException("Keycloak не вернул id созданного пользователя");
        }
        var path = location.getPath();
        return UUID.fromString(path.substring(path.lastIndexOf('/') + 1));
    }

    private String adminToken() {
        var form = new LinkedMultiValueMap<String, String>();
        form.add("grant_type", "password");
        form.add("client_id", "admin-cli");
        form.add("username", adminUsername);
        form.add("password", adminPassword);

        @SuppressWarnings("unchecked")
        var response = keycloak.post()
                .uri("/realms/master/protocol/openid-connect/token")
                .contentType(org.springframework.http.MediaType.APPLICATION_FORM_URLENCODED)
                .body(form)
                .retrieve()
                .body(Map.class);

        if (response == null || response.get("access_token") == null) {
            throw new IllegalStateException("Keycloak admin token is empty");
        }
        return response.get("access_token").toString();
    }
}
