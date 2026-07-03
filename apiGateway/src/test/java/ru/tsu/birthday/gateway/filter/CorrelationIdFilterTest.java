package ru.tsu.birthday.gateway.filter;

import org.junit.jupiter.api.Test;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import java.util.concurrent.atomic.AtomicReference;
import static org.assertj.core.api.Assertions.assertThat;

class CorrelationIdFilterTest {
    private final CorrelationIdFilter filter = new CorrelationIdFilter();

    @Test
    void preservesValidIncomingCorrelationId() {
        var exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/api/users")
                        .header(CorrelationIdFilter.HEADER_NAME, "request-42")
        );
        var capturedHeader = new AtomicReference<String>();

        StepVerifier.create(filter.filter(exchange, currentExchange -> {
                    capturedHeader.set(currentExchange.getRequest().getHeaders()
                            .getFirst(CorrelationIdFilter.HEADER_NAME));
                    return Mono.empty();
                }))
                .verifyComplete();

        assertThat(capturedHeader).hasValue("request-42");
        assertThat(exchange.getResponse().getHeaders().getFirst(CorrelationIdFilter.HEADER_NAME))
                .isEqualTo("request-42");
    }

    @Test
    void generatesCorrelationIdWhenHeaderIsMissing() {
        var exchange = MockServerWebExchange.from(MockServerHttpRequest.get("/api/users"));
        var capturedHeader = new AtomicReference<String>();

        StepVerifier.create(filter.filter(exchange, currentExchange -> {
                    capturedHeader.set(currentExchange.getRequest().getHeaders()
                            .getFirst(CorrelationIdFilter.HEADER_NAME));
                    return Mono.empty();
                }))
                .verifyComplete();

        assertThat(capturedHeader.get()).isNotBlank();
    }
}
