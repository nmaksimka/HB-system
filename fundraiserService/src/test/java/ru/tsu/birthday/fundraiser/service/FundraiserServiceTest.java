package ru.tsu.birthday.fundraiser.service;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import ru.tsu.birthday.fundraiser.client.BankClient;
import ru.tsu.birthday.fundraiser.client.GiftClient;
import ru.tsu.birthday.fundraiser.client.OpenCollectionRequest;
import ru.tsu.birthday.fundraiser.client.WishSummary;
import ru.tsu.birthday.fundraiser.dto.CreateFundraiserRequest;
import ru.tsu.birthday.fundraiser.entity.Contribution;
import ru.tsu.birthday.fundraiser.entity.Fundraiser;
import ru.tsu.birthday.fundraiser.entity.FundraiserStatus;
import ru.tsu.birthday.fundraiser.entity.ProcessedEvent;
import ru.tsu.birthday.fundraiser.event.FundraiserClosed;
import ru.tsu.birthday.fundraiser.event.Outbox;
import ru.tsu.birthday.fundraiser.event.PaymentRejected;
import ru.tsu.birthday.fundraiser.event.PaymentSucceeded;
import ru.tsu.birthday.fundraiser.repository.ContributionRepository;
import ru.tsu.birthday.fundraiser.repository.FundraiserRepository;
import ru.tsu.birthday.fundraiser.repository.ProcessedEventRepository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class FundraiserServiceTest {
    private final FundraiserRepository fundraisers = mock(FundraiserRepository.class);
    private final ContributionRepository contributions = mock(ContributionRepository.class);
    private final ProcessedEventRepository processedEvents = mock(ProcessedEventRepository.class);
    private final Outbox outbox = mock(Outbox.class);
    private final GiftClient giftClient = mock(GiftClient.class);
    private final BankClient bankClient = mock(BankClient.class);
    private final TransactionTemplate transaction = new TransactionTemplate(mock(PlatformTransactionManager.class));
    private final Instant now = Instant.parse("2026-01-01T10:00:00Z");
    private final Clock clock = Clock.fixed(now, ZoneOffset.UTC);
    private final FundraiserService service = new FundraiserService(fundraisers, contributions, processedEvents,
            outbox, giftClient, bankClient, transaction, clock);

    private final UUID ownerId = UUID.randomUUID();
    private final UUID beneficiaryId = UUID.randomUUID();
    private final UUID wishId = UUID.randomUUID();

    @Test
    void rejectsCollectingMoneyForOwnGift() {
        when(giftClient.wish(wishId)).thenReturn(new WishSummary(wishId, ownerId, "Book"));

        assertThatThrownBy(() -> service.create(ownerId, request()))
                .isInstanceOf(AccessDeniedException.class);
        verifyNoInteractions(bankClient);
        verify(fundraisers, never()).save(any());
    }

    @Test
    void rejectsSecondActiveFundraiserForSameWish() {
        when(giftClient.wish(wishId)).thenReturn(new WishSummary(wishId, beneficiaryId, "Book"));
        when(fundraisers.existsByWishIdAndStatus(wishId, FundraiserStatus.ACTIVE)).thenReturn(true);

        assertThatThrownBy(() -> service.create(ownerId, request()))
                .isInstanceOf(IllegalStateException.class);
        verifyNoInteractions(bankClient);
        verify(fundraisers, never()).save(any());
    }

    @Test
    void opensBankCollectionBeforeSavingFundraiser() {
        when(giftClient.wish(wishId)).thenReturn(new WishSummary(wishId, beneficiaryId, "Book"));
        when(fundraisers.existsByWishIdAndStatus(wishId, FundraiserStatus.ACTIVE)).thenReturn(false);
        when(fundraisers.save(any(Fundraiser.class))).thenAnswer(call -> call.getArgument(0));

        var created = service.create(ownerId, request());

        var order = inOrder(bankClient, fundraisers);
        var opened = ArgumentCaptor.forClass(OpenCollectionRequest.class);
        order.verify(bankClient).openCollection(opened.capture());
        order.verify(fundraisers).save(created);
        assertThat(opened.getValue().fundraiserId()).isEqualTo(created.getId());
        assertThat(opened.getValue().beneficiaryId()).isEqualTo(beneficiaryId);
        assertThat(opened.getValue().targetAmount()).isEqualByComparingTo("500.00");
    }

    @Test
    void doesNotSaveFundraiserWhenBankFails() {
        when(giftClient.wish(wishId)).thenReturn(new WishSummary(wishId, beneficiaryId, "Book"));
        when(fundraisers.existsByWishIdAndStatus(wishId, FundraiserStatus.ACTIVE)).thenReturn(false);
        doThrow(new IllegalStateException("bank is down")).when(bankClient).openCollection(any());

        assertThatThrownBy(() -> service.create(ownerId, request()))
                .isInstanceOf(IllegalStateException.class);
        verify(fundraisers, never()).save(any());
    }

    @Test
    void createsActiveFundraiserWithTrimmedTitle() {
        when(giftClient.wish(wishId)).thenReturn(new WishSummary(wishId, beneficiaryId, "Book"));
        when(fundraisers.save(any(Fundraiser.class))).thenAnswer(call -> call.getArgument(0));

        var created = service.create(ownerId, new CreateFundraiserRequest(wishId, "  Book for Anna  ",
                new BigDecimal("500.00")));

        assertThat(created.getTitle()).isEqualTo("Book for Anna");
        assertThat(created.getOwnerId()).isEqualTo(ownerId);
        assertThat(created.getBeneficiaryId()).isEqualTo(beneficiaryId);
        assertThat(created.getWishId()).isEqualTo(wishId);
        assertThat(created.isActive()).isTrue();
        assertThat(created.getCreatedAt()).isEqualTo(now);
    }

    @Test
    void hidesFundraisersFromBirthdayPerson() {
        assertThatThrownBy(() -> service.forBeneficiary(beneficiaryId, beneficiaryId))
                .isInstanceOf(AccessDeniedException.class);
        verifyNoInteractions(fundraisers);
    }

    @Test
    void listsFundraisersForOtherViewer() {
        var fundraiser = activeFundraiser("100.00");
        when(fundraisers.findByBeneficiaryIdOrderByCreatedAtDesc(beneficiaryId)).thenReturn(List.of(fundraiser));

        assertThat(service.forBeneficiary(ownerId, beneficiaryId)).containsExactly(fundraiser);
    }

    @Test
    void hidesFundraiserFromBirthdayPersonOnGet() {
        var fundraiser = activeFundraiser("100.00");
        when(fundraisers.findById(fundraiser.getId())).thenReturn(Optional.of(fundraiser));

        assertThatThrownBy(() -> service.get(beneficiaryId, fundraiser.getId()))
                .isInstanceOf(NoSuchElementException.class);
    }

    @Test
    void returnsFundraiserToOtherViewer() {
        var fundraiser = activeFundraiser("100.00");
        when(fundraisers.findById(fundraiser.getId())).thenReturn(Optional.of(fundraiser));

        assertThat(service.get(UUID.randomUUID(), fundraiser.getId())).isSameAs(fundraiser);
    }

    @Test
    void rejectsGettingMissingFundraiser() {
        var id = UUID.randomUUID();
        when(fundraisers.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.get(ownerId, id)).isInstanceOf(NoSuchElementException.class);
    }

    @Test
    void rejectsCancellingByNonOwner() {
        var fundraiser = activeFundraiser("100.00");
        when(fundraisers.findById(fundraiser.getId())).thenReturn(Optional.of(fundraiser));

        assertThatThrownBy(() -> service.cancel(UUID.randomUUID(), fundraiser.getId()))
                .isInstanceOf(AccessDeniedException.class);
        assertThat(fundraiser.isActive()).isTrue();
        verifyNoInteractions(outbox);
    }

    @Test
    void cancelsOwnFundraiserAndPublishesClosedEvent() {
        var fundraiser = activeFundraiser("100.00");
        when(fundraisers.findById(fundraiser.getId())).thenReturn(Optional.of(fundraiser));

        var cancelled = service.cancel(ownerId, fundraiser.getId());

        var event = ArgumentCaptor.forClass(FundraiserClosed.class);
        verify(outbox).fundraiserClosed(event.capture());
        assertThat(cancelled.getStatus()).isEqualTo(FundraiserStatus.CANCELLED);
        assertThat(event.getValue().fundraiserId()).isEqualTo(fundraiser.getId());
        assertThat(event.getValue().status()).isEqualTo(FundraiserStatus.CANCELLED);
        assertThat(event.getValue().closedAt()).isEqualTo(now);
    }

    @Test
    void rejectsCancellingAlreadyClosedFundraiser() {
        var fundraiser = activeFundraiser("100.00");
        fundraiser.cancel(now);
        when(fundraisers.findById(fundraiser.getId())).thenReturn(Optional.of(fundraiser));

        assertThatThrownBy(() -> service.cancel(ownerId, fundraiser.getId()))
                .isInstanceOf(IllegalStateException.class);
        verifyNoInteractions(outbox);
    }

    @Test
    void ignoresAlreadyProcessedPayment() {
        var payment = payment(UUID.randomUUID(), "50.00");
        when(processedEvents.existsById(payment.eventId())).thenReturn(true);

        service.applyPayment(payment);

        verify(processedEvents, never()).save(any());
        verifyNoInteractions(contributions, outbox);
    }

    @Test
    void addsContributionForActiveFundraiser() {
        var fundraiser = activeFundraiser("100.00");
        var payment = payment(fundraiser.getId(), "40.00");
        when(processedEvents.existsById(payment.eventId())).thenReturn(false);
        when(fundraisers.findById(fundraiser.getId())).thenReturn(Optional.of(fundraiser));

        service.applyPayment(payment);

        var saved = ArgumentCaptor.forClass(Contribution.class);
        verify(contributions).save(saved.capture());
        verify(processedEvents).save(any(ProcessedEvent.class));
        assertThat(saved.getValue().getPaymentId()).isEqualTo(payment.paymentId());
        assertThat(saved.getValue().getFundraiserId()).isEqualTo(fundraiser.getId());
        assertThat(saved.getValue().getPayerId()).isEqualTo(payment.payerId());
        assertThat(saved.getValue().getAmount()).isEqualByComparingTo("40.00");
        assertThat(fundraiser.getCollectedAmount()).isEqualByComparingTo("40.00");
        assertThat(fundraiser.isActive()).isTrue();
        verifyNoInteractions(outbox);
    }

    @Test
    void publishesClosedEventWhenTargetIsReached() {
        var fundraiser = activeFundraiser("100.00");
        var payment = payment(fundraiser.getId(), "100.00");
        when(fundraisers.findById(fundraiser.getId())).thenReturn(Optional.of(fundraiser));

        service.applyPayment(payment);

        var event = ArgumentCaptor.forClass(FundraiserClosed.class);
        verify(outbox).fundraiserClosed(event.capture());
        assertThat(fundraiser.getStatus()).isEqualTo(FundraiserStatus.COMPLETED);
        assertThat(event.getValue().fundraiserId()).isEqualTo(fundraiser.getId());
        assertThat(event.getValue().status()).isEqualTo(FundraiserStatus.COMPLETED);
        assertThat(event.getValue().collectedAmount()).isEqualByComparingTo("100.00");
    }

    @Test
    void rejectsPaymentForClosedFundraiser() {
        var fundraiser = activeFundraiser("100.00");
        fundraiser.cancel(now);
        var payment = payment(fundraiser.getId(), "40.00");
        when(fundraisers.findById(fundraiser.getId())).thenReturn(Optional.of(fundraiser));

        service.applyPayment(payment);

        var event = ArgumentCaptor.forClass(PaymentRejected.class);
        verify(outbox).paymentRejected(event.capture());
        verify(contributions, never()).save(any());
        assertThat(fundraiser.getCollectedAmount()).isEqualByComparingTo("0");
        assertThat(event.getValue().paymentId()).isEqualTo(payment.paymentId());
        assertThat(event.getValue().fundraiserId()).isEqualTo(fundraiser.getId());
    }

    @Test
    void rejectsPaymentForMissingFundraiser() {
        var payment = payment(UUID.randomUUID(), "40.00");
        when(fundraisers.findById(payment.fundraiserId())).thenReturn(Optional.empty());

        service.applyPayment(payment);

        var event = ArgumentCaptor.forClass(PaymentRejected.class);
        verify(outbox).paymentRejected(event.capture());
        verify(contributions, never()).save(any());
        assertThat(event.getValue().paymentId()).isEqualTo(payment.paymentId());
        assertThat(event.getValue().fundraiserId()).isEqualTo(payment.fundraiserId());
    }

    @Test
    void rejectedPaymentIsMarkedAsProcessed() {
        var payment = payment(UUID.randomUUID(), "40.00");
        when(fundraisers.findById(payment.fundraiserId())).thenReturn(Optional.empty());

        service.applyPayment(payment);

        verify(processedEvents).save(any(ProcessedEvent.class));
    }

    private CreateFundraiserRequest request() {
        return new CreateFundraiserRequest(wishId, "Book", new BigDecimal("500.00"));
    }

    private Fundraiser activeFundraiser(String target) {
        return new Fundraiser(UUID.randomUUID(), ownerId, beneficiaryId, wishId, "Book", new BigDecimal(target), now);
    }

    private PaymentSucceeded payment(UUID fundraiserId, String amount) {
        return new PaymentSucceeded(UUID.randomUUID(), UUID.randomUUID(), fundraiserId, UUID.randomUUID(),
                new BigDecimal(amount), now);
    }
}
