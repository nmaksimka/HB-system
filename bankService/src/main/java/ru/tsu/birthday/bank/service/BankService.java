package ru.tsu.birthday.bank.service;

import java.time.Clock;
import java.time.Instant;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.tsu.birthday.bank.dto.CollectionResponse;
import ru.tsu.birthday.bank.dto.OpenCollectionRequest;
import ru.tsu.birthday.bank.dto.PayRequest;
import ru.tsu.birthday.bank.entity.Collection;
import ru.tsu.birthday.bank.entity.OutboxEvent;
import ru.tsu.birthday.bank.entity.Payment;
import ru.tsu.birthday.bank.entity.ProcessedEvent;
import ru.tsu.birthday.bank.event.FundraiserClosed;
import ru.tsu.birthday.bank.event.PaymentSucceeded;
import ru.tsu.birthday.bank.repository.CollectionRepository;
import ru.tsu.birthday.bank.repository.OutboxEventRepository;
import ru.tsu.birthday.bank.repository.PaymentRepository;
import ru.tsu.birthday.bank.repository.ProcessedEventRepository;
import tools.jackson.databind.json.JsonMapper;

@Service
@Transactional
public class BankService {
    private final CollectionRepository collections;
    private final PaymentRepository payments;
    private final OutboxEventRepository outbox;
    private final ProcessedEventRepository processedEvents;
    private final JsonMapper json;
    private final Clock clock;

    public BankService(CollectionRepository collections, PaymentRepository payments, OutboxEventRepository outbox,
                       ProcessedEventRepository processedEvents, JsonMapper json, Clock clock) {
        this.collections = collections;
        this.payments = payments;
        this.outbox = outbox;
        this.processedEvents = processedEvents;
        this.json = json;
        this.clock = clock;
    }

    public void open(OpenCollectionRequest request) {
        if (!collections.existsById(request.fundraiserId())) {
            collections.save(new Collection(request.fundraiserId(), request.beneficiaryId(), request.targetAmount(),
                    Instant.now(clock)));
        }
    }

    @Transactional(readOnly = true)
    public CollectionResponse collection(UUID viewerId, UUID fundraiserId) {
        var collection = visibleCollection(viewerId, collections.findById(fundraiserId), fundraiserId);
        return new CollectionResponse(fundraiserId, collection.getTargetAmount(), payments.totalPaid(fundraiserId),
                collection.isOpen());
    }

    public Payment pay(UUID payerId, UUID fundraiserId, PayRequest request) {
        var repeated = payments.findByPayerIdAndIdempotencyKey(payerId, request.idempotencyKey());
        if (repeated.isPresent()) {
            if (!repeated.get().getFundraiserId().equals(fundraiserId)) {
                throw new IllegalStateException("Idempotency key was already used for another payment");
            }
            return repeated.get();
        }
        var collection = visibleCollection(payerId, collections.lockById(fundraiserId), fundraiserId);
        if (!collection.isOpen()) {
            throw new IllegalStateException("Collection is closed");
        }
        var left = collection.getTargetAmount().subtract(payments.totalPaid(fundraiserId));
        if (request.amount().compareTo(left) > 0) {
            throw new IllegalArgumentException("Only " + left.toPlainString() + " is left to collect");
        }
        var now = Instant.now(clock);
        var payment = payments.save(new Payment(UUID.randomUUID(), fundraiserId, payerId, request.amount(),
                request.idempotencyKey(), now));
        var event = new PaymentSucceeded(UUID.randomUUID(), payment.getId(), fundraiserId, payerId,
                payment.getAmount(), now);
        outbox.save(new OutboxEvent(event.eventId(), PaymentSucceeded.TOPIC, fundraiserId,
                json.writeValueAsString(event), now));
        return payment;
    }

    public void closeCollection(FundraiserClosed event) {
        if (processedEvents.existsById(event.eventId())) {
            return;
        }
        processedEvents.save(new ProcessedEvent(event.eventId(), Instant.now(clock)));
        collections.findById(event.fundraiserId())
                .filter(Collection::isOpen)
                .ifPresent(collection -> collection.close(event.closedAt()));
    }

    private Collection visibleCollection(UUID viewerId, Optional<Collection> collection, UUID fundraiserId) {
        return collection
                .filter(c -> !c.getBeneficiaryId().equals(viewerId))
                .orElseThrow(() -> new NoSuchElementException("Collection not found: " + fundraiserId));
    }
}
