package ru.tsu.birthday.chat.security;

import java.security.Principal;
import java.util.UUID;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
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
        return parse(rawId);
    }

    public UUID id(Principal principal) {
        if (principal instanceof JwtAuthenticationToken token) {
            return id(token.getToken());
        }
        if (principal instanceof Authentication authentication) {
            return parse(authentication.getName());
        }
        throw new AccessDeniedException("Authenticated user is required");
    }

    private UUID parse(String rawId) {
        try {
            return UUID.fromString(rawId);
        } catch (RuntimeException ex) {
            throw new AccessDeniedException("Authenticated user id must be a UUID", ex);
        }
    }
}
