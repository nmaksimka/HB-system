package ru.tsu.birthday.gateway.config;

import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.RouterFunctions;
import org.springframework.web.reactive.function.server.ServerResponse;

@Configuration
public class AuthConfigRoute {
    @Bean
    RouterFunction<ServerResponse> authConfig(
            @Value("${spring.security.oauth2.resourceserver.jwt.issuer-uri}") String issuer,
            @Value("${birthday.ui.client-id:birthday-hub-ui}") String clientId) {
        var body = Map.of("tokenUrl", issuer + "/protocol/openid-connect/token", "clientId", clientId);
        return RouterFunctions.route()
                .GET("/auth/config", request -> ServerResponse.ok().bodyValue(body))
                .build();
    }
}
