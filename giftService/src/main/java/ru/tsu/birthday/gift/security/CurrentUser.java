package ru.tsu.birthday.gift.security;

import java.util.UUID;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class CurrentUser {
    public UUID id(Jwt jwt) {
        if (jwt == null) {
            throw new AccessDeniedException("Authenticated user is required");
        }
        var rawId = jwt.getClaimAsString("user_id");
        if (!StringUtils.hasText(rawId)) {
            rawId = jwt.getSubject();
        }
        try {
            return UUID.fromString(rawId);
        } catch (RuntimeException ex) {
            throw new AccessDeniedException("Authenticated user id must be a UUID", ex);
        }
    }
}
