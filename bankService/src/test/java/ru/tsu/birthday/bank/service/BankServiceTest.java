package ru.tsu.birthday.bank.service;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import ru.tsu.birthday.bank.dto.OpenCollectionRequest;
import ru.tsu.birthday.bank.dto.PayRequest;
import ru.tsu.birthday.bank.entity.Collection;
import ru.tsu.birthday.bank.entity.OutboxEvent;
import ru.tsu.birthday.bank.entity.Payment;
import ru.tsu.birthday.bank.entity.ProcessedEvent;
import ru.tsu.birthday.bank.event.FundraiserClosed;
import ru.tsu.birthday.bank.event.PaymentRejected;
import ru.tsu.birthday.bank.event.PaymentSucceeded;
import ru.tsu.birthday.bank.repository.CollectionRepository;
import ru.tsu.birthday.bank.repository.OutboxEventRepository;
import ru.tsu.birthday.bank.repository.PaymentRepository;
import ru.tsu.birthday.bank.repository.ProcessedEventRepository;
import tools.jackson.databind.json.JsonMapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class BankServiceTest {
    private final CollectionRepository collections = mock(CollectionRepository.class);
    private final PaymentRepository payments = mock(PaymentRepository.class);
    private final OutboxEventRepository outbox = mock(OutboxEventRepository.class);
    private final ProcessedEventRepository processedEvents = mock(ProcessedEventRepository.class);
    private final JsonMapper json = JsonMapper.builder().build();
    private final Instant now = Instant.parse("2026-01-01T10:00:00Z");
    private final Clock clock = Clock.fixed(now, ZoneOffset.UTC);
    private final BankService service = new BankService(collections, payments, outbox, processedEvents, json, clock);

    private final UUID fundraiserId = UUID.randomUUID();
    private final UUID beneficiaryId = UUID.randomUUID();
    private final UUID payerId = UUID.randomUUID();

    @Test
    void opensNewCollection() {
        when(collections.existsById(fundraiserId)).thenReturn(false);

        service.open(new OpenCollectionRequest(fundraiserId, beneficiaryId, new BigDecimal("500.00")));

        var saved = ArgumentCaptor.forClass(Collection.class);
        verify(collections).save(saved.capture());
        assertThat(saved.getValue().getFundraiserId()).isEqualTo(fundraiserId);
        assertThat(saved.getValue().getBeneficiaryId()).isEqualTo(beneficiaryId);
        assertThat(saved.getValue().getTargetAmount()).isEqualByComparingTo("500.00");
        assertThat(saved.getValue().isOpen()).isTrue();
        assertThat(saved.getValue().getOpenedAt()).isEqualTo(now);
    }

    @Test
    void openingExistingCollectionIsIdempotent() {
        when(collections.existsById(fundraiserId)).thenReturn(true);

        service.open(new OpenCollectionRequest(fundraiserId, beneficiaryId, new BigDecimal("500.00")));

        verify(collections, never()).save(any());
    }

    @Test
    void returnsSamePaymentForRepeatedKey() {
        var existing = payment(fundraiserId, "100.00", "key-1");
        when(payments.findByPayerIdAndIdempotencyKey(payerId, "key-1")).thenReturn(Optional.of(existing));

        var result = service.pay(payerId, fundraiserId, new PayRequest(new BigDecimal("100.00"), "key-1"));

        assertThat(result).isSameAs(existing);
        verify(payments, never()).save(any());
        verify(outbox, never()).save(any());
    }

    @Test
    void rejectsSameKeyForAnotherFundraiser() {
        var existing = payment(UUID.randomUUID(), "100.00", "key-1");
        when(payments.findByPayerIdAndIdempotencyKey(payerId, "key-1")).thenReturn(Optional.of(existing));

        assertThatIllegalStateException()
                .isThrownBy(() -> service.pay(payerId, fundraiserId, new PayRequest(new BigDecimal("100.00"), "key-1")));
        verify(payments, never()).save(any());
    }

    @Test
    void hidesCollectionFromBirthdayPerson() {
        stubCollection("500.00", "0");

        assertThatThrownBy(() -> service.pay(beneficiaryId, fundraiserId,
                new PayRequest(new BigDecimal("100.00"), "key-1")))
                .isInstanceOf(NoSuchElementException.class);
        verify(payments, never()).save(any());
    }

    @Test
    void rejectsPaymentToMissingCollection() {
        when(collections.lockById(fundraiserId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.pay(payerId, fundraiserId,
                new PayRequest(new BigDecimal("100.00"), "key-1")))
                .isInstanceOf(NoSuchElementException.class);
    }

    @Test
    void rejectsPaymentToClosedCollection() {
        var collection = stubCollection("500.00", "0");
        collection.close(now);

        assertThatIllegalStateException()
                .isThrownBy(() -> service.pay(payerId, fundraiserId,
                        new PayRequest(new BigDecimal("100.00"), "key-1")));
        verify(payments, never()).save(any());
    }

    @Test
    void rejectsAmountGreaterThanRemainder() {
        stubCollection("500.00", "450.00");

        assertThatIllegalArgumentException()
                .isThrownBy(() -> service.pay(payerId, fundraiserId,
                        new PayRequest(new BigDecimal("50.01"), "key-1")))
                .withMessageContaining("50.00");
        verify(payments, never()).save(any());
    }

    @Test
    void acceptsAmountEqualToRemainder() {
        stubCollection("500.00", "450.00");
        when(payments.save(any(Payment.class))).thenAnswer(call -> call.getArgument(0));

        var payment = service.pay(payerId, fundraiserId, new PayRequest(new BigDecimal("50.00"), "key-1"));

        assertThat(payment.getAmount()).isEqualByComparingTo("50.00");
    }

    @Test
    void savesPaymentAndPaymentSucceededEvent() {
        stubCollection("500.00", "0");
        when(payments.save(any(Payment.class))).thenAnswer(call -> call.getArgument(0));

        var payment = service.pay(payerId, fundraiserId, new PayRequest(new BigDecimal("100.00"), "key-1"));

        var saved = ArgumentCaptor.forClass(OutboxEvent.class);
        verify(outbox).save(saved.capture());
        var event = json.readValue(saved.getValue().getPayload(), PaymentSucceeded.class);
        assertThat(payment.getPayerId()).isEqualTo(payerId);
        assertThat(payment.getFundraiserId()).isEqualTo(fundraiserId);
        assertThat(payment.getIdempotencyKey()).isEqualTo("key-1");
        assertThat(payment.getPaidAt()).isEqualTo(now);
        assertThat(saved.getValue().getTopic()).isEqualTo("payment.succeeded");
        assertThat(saved.getValue().getAggregateId()).isEqualTo(fundraiserId);
        assertThat(saved.getValue().getId()).isEqualTo(event.eventId());
        assertThat(event.paymentId()).isEqualTo(payment.getId());
        assertThat(event.fundraiserId()).isEqualTo(fundraiserId);
        assertThat(event.payerId()).isEqualTo(payerId);
        assertThat(event.amount()).isEqualByComparingTo("100.00");
    }

    @Test
    void closesOpenCollectionOnFundraiserClosed() {
        var collection = new Collection(fundraiserId, beneficiaryId, new BigDecimal("500.00"), now);
        var closedAt = Instant.parse("2026-01-02T10:00:00Z");
        var event = new FundraiserClosed(UUID.randomUUID(), fundraiserId, "COMPLETED", new BigDecimal("500.00"),
                closedAt);
        when(processedEvents.existsById(event.eventId())).thenReturn(false);
        when(collections.findById(fundraiserId)).thenReturn(Optional.of(collection));

        service.closeCollection(event);

        verify(processedEvents).save(any(ProcessedEvent.class));
        assertThat(collection.isOpen()).isFalse();
        assertThat(collection.getClosedAt()).isEqualTo(closedAt);
    }

    @Test
    void closingCollectionIsIdempotentByEventId() {
        var event = new FundraiserClosed(UUID.randomUUID(), fundraiserId, "COMPLETED", new BigDecimal("500.00"), now);
        when(processedEvents.existsById(event.eventId())).thenReturn(true);

        service.closeCollection(event);

        verify(processedEvents, never()).save(any());
        verify(collections, never()).findById(any());
    }

    @Test
    void closingMissingCollectionOnlyMarksEventProcessed() {
        var event = new FundraiserClosed(UUID.randomUUID(), fundraiserId, "CANCELLED", BigDecimal.ZERO, now);
        when(collections.findById(fundraiserId)).thenReturn(Optional.empty());

        service.closeCollection(event);

        verify(processedEvents).save(any(ProcessedEvent.class));
    }

    @Test
    void keepsFirstCloseTimeWhenCollectionIsAlreadyClosed() {
        var collection = new Collection(fundraiserId, beneficiaryId, new BigDecimal("500.00"), now);
        var firstClose = Instant.parse("2026-01-02T10:00:00Z");
        collection.close(firstClose);
        var event = new FundraiserClosed(UUID.randomUUID(), fundraiserId, "CANCELLED", BigDecimal.ZERO,
                Instant.parse("2026-01-03T10:00:00Z"));
        when(collections.findById(fundraiserId)).thenReturn(Optional.of(collection));

        service.closeCollection(event);

        assertThat(collection.getClosedAt()).isEqualTo(firstClose);
    }

    @Test
    void refundsPaymentOnPaymentRejected() {
        var payment = payment(fundraiserId, "100.00", "key-1");
        var event = new PaymentRejected(UUID.randomUUID(), payment.getId(), fundraiserId);
        when(processedEvents.existsById(event.eventId())).thenReturn(false);
        when(payments.findById(payment.getId())).thenReturn(Optional.of(payment));

        service.refund(event);

        verify(processedEvents).save(any(ProcessedEvent.class));
        assertThat(payment.getRefundedAt()).isEqualTo(now);
    }

    @Test
    void refundIsIdempotentByEventId() {
        var payment = payment(fundraiserId, "100.00", "key-1");
        var event = new PaymentRejected(UUID.randomUUID(), payment.getId(), fundraiserId);
        when(processedEvents.existsById(event.eventId())).thenReturn(true);
        when(payments.findById(payment.getId())).thenReturn(Optional.of(payment));

        service.refund(event);

        verify(processedEvents, never()).save(any());
        assertThat(payment.getRefundedAt()).isNull();
    }

    @Test
    void refundOfMissingPaymentOnlyMarksEventProcessed() {
        var event = new PaymentRejected(UUID.randomUUID(), UUID.randomUUID(), fundraiserId);
        when(payments.findById(event.paymentId())).thenReturn(Optional.empty());

        service.refund(event);

        verify(processedEvents).save(any(ProcessedEvent.class));
    }

    @Test
    void secondRefundKeepsFirstRefundTime() {
        var payment = payment(fundraiserId, "100.00", "key-1");
        payment.refund(now);

        payment.refund(Instant.parse("2026-02-01T10:00:00Z"));

        assertThat(payment.getRefundedAt()).isEqualTo(now);
    }

    private Collection stubCollection(String target, String alreadyPaid) {
        var collection = new Collection(fundraiserId, beneficiaryId, new BigDecimal(target), now);
        when(collections.lockById(fundraiserId)).thenReturn(Optional.of(collection));
        when(payments.totalPaid(fundraiserId)).thenReturn(new BigDecimal(alreadyPaid));
        return collection;
    }

    private Payment payment(UUID fundraiser, String amount, String key) {
        return new Payment(UUID.randomUUID(), fundraiser, payerId, new BigDecimal(amount), key, now);
    }
}
