package ru.tsu.birthday.identity.config;

import org.springframework.context.annotation.*;
import java.time.Clock;

@Configuration(proxyBeanMethods = false)
public class TimeConfig {
    @Bean
    Clock clock() {
        return Clock.systemUTC();
    }
}
