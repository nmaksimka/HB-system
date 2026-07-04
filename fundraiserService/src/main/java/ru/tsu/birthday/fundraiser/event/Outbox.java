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
    public void fundraiserClosed(UUID eventId, FundraiserClosed event) {
        repository.save(new OutboxEvent(eventId, FundraiserClosed.TOPIC, event.fundraiserId(),
                json.writeValueAsString(event), Instant.now(clock)));
    }
}
