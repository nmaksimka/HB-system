package ru.tsu.birthday.bank.config;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authorization.AuthorizationDecision;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class SecurityConfiguration {
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http, @Value("${internal.token}") String internalToken)
            throws Exception {
        var expected = internalToken.getBytes(StandardCharsets.UTF_8);
        http.authorizeHttpRequests(authorizeHttpRequests -> authorizeHttpRequests
                .requestMatchers("/actuator/health/**", "/actuator/info").permitAll()
                .requestMatchers("/internal/**").access((authentication, context) -> {
                    var header = context.getRequest().getHeader("X-Internal-Token");
                    return new AuthorizationDecision(header != null
                            && MessageDigest.isEqual(expected, header.getBytes(StandardCharsets.UTF_8)));
                })
                .anyRequest().authenticated()
        );
        http.oauth2ResourceServer(oauth2ResourceServer -> oauth2ResourceServer
                .jwt(Customizer.withDefaults())
        );
        http.csrf(csrf -> csrf.disable());
        return http.build();
    }
}
