package ru.tsu.birthday.notification.config;

import feign.RequestInterceptor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class SecurityConfiguration {
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http.authorizeHttpRequests(authorizeHttpRequests -> authorizeHttpRequests
                .requestMatchers("/actuator/health/**", "/actuator/info").permitAll()
                .anyRequest().authenticated()
        );
        http.oauth2ResourceServer(oauth2ResourceServer -> oauth2ResourceServer
                .jwt(Customizer.withDefaults())
        );
        http.csrf(csrf -> csrf.disable());
        return http.build();
    }

    @Bean
    RequestInterceptor internalTokenInterceptor(@Value("${internal.token}") String token) {
        return template -> template.header("X-Internal-Token", token);
    }
}
