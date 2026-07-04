package ru.tsu.birthday.fundraiser.event;

import java.time.Clock;
import java.time.Instant;
import java.util.UUID;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import ru.tsu.birthday.fundraiser.entity.OutboxEvent;
import ru.tsu.birthday.fundraiser.repository.OutboxEventRepository;
import tools.jackson.databind.json.JsonMapper;

@Component
public class Outbox {
    private final OutboxEventRepository repository;
    private final JsonMapper json;
    private final Clock clock;

    public Outbox(OutboxEventRepository repository, JsonMapper json, Clock clock) {
        this.repository = repository;
        this.json = json;
        this.clock = clock;
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public void fundraiserClosed(FundraiserClosed event) {
        save(event.eventId(), FundraiserClosed.TOPIC, event.fundraiserId(), event);
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public void paymentRejected(PaymentRejected event) {
        save(event.eventId(), PaymentRejected.TOPIC, event.fundraiserId(), event);
    }

    private void save(UUID eventId, String topic, UUID aggregateId, Object event) {
        repository.save(new OutboxEvent(eventId, topic, aggregateId, json.writeValueAsString(event),
                Instant.now(clock)));
    }
}
