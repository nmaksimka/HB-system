package ru.tsu.birthday.fundraiser.service;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.UUID;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;
import ru.tsu.birthday.fundraiser.client.BankClient;
import ru.tsu.birthday.fundraiser.client.GiftClient;
import ru.tsu.birthday.fundraiser.client.OpenCollectionRequest;
import ru.tsu.birthday.fundraiser.dto.CreateFundraiserRequest;
import ru.tsu.birthday.fundraiser.entity.Contribution;
import ru.tsu.birthday.fundraiser.entity.Fundraiser;
import ru.tsu.birthday.fundraiser.entity.FundraiserStatus;
import ru.tsu.birthday.fundraiser.entity.ProcessedEvent;
import ru.tsu.birthday.fundraiser.event.FundraiserClosed;
import ru.tsu.birthday.fundraiser.event.Outbox;
import ru.tsu.birthday.fundraiser.event.PaymentSucceeded;
import ru.tsu.birthday.fundraiser.repository.ContributionRepository;
import ru.tsu.birthday.fundraiser.repository.FundraiserRepository;
import ru.tsu.birthday.fundraiser.repository.ProcessedEventRepository;

@Service
public class FundraiserService {
    private final FundraiserRepository fundraisers;
    private final ContributionRepository contributions;
    private final ProcessedEventRepository processedEvents;
    private final Outbox outbox;
    private final GiftClient giftClient;
    private final BankClient bankClient;
    private final TransactionTemplate transaction;
    private final Clock clock;

    public FundraiserService(FundraiserRepository fundraisers, ContributionRepository contributions,
                             ProcessedEventRepository processedEvents, Outbox outbox, GiftClient giftClient,
                             BankClient bankClient, TransactionTemplate transaction, Clock clock) {
        this.fundraisers = fundraisers;
        this.contributions = contributions;
        this.processedEvents = processedEvents;
        this.outbox = outbox;
        this.giftClient = giftClient;
        this.bankClient = bankClient;
        this.transaction = transaction;
        this.clock = clock;
    }

    public Fundraiser create(UUID ownerId, CreateFundraiserRequest request) {
        var wish = giftClient.wish(request.wishId());
        if (wish.userId().equals(ownerId)) {
            throw new AccessDeniedException("You cannot collect money for your own gift");
        }
        if (fundraisers.existsByWishIdAndStatus(wish.id(), FundraiserStatus.ACTIVE)) {
            throw new IllegalStateException("Money for this gift is already being collected");
        }
        var fundraiser = new Fundraiser(UUID.randomUUID(), ownerId, wish.userId(), wish.id(),
                request.title().trim(), request.targetAmount(), Instant.now(clock));
        bankClient.openCollection(new OpenCollectionRequest(
                fundraiser.getId(), fundraiser.getBeneficiaryId(), fundraiser.getTargetAmount()));
        return transaction.execute(status -> fundraisers.save(fundraiser));
    }

    public List<Fundraiser> forBeneficiary(UUID viewerId, UUID beneficiaryId) {
        if (viewerId.equals(beneficiaryId)) {
            throw new AccessDeniedException("The birthday person cannot see fundraisers for their gifts");
        }
        return fundraisers.findByBeneficiaryIdOrderByCreatedAtDesc(beneficiaryId);
    }

    public Fundraiser get(UUID viewerId, UUID fundraiserId) {
        var fundraiser = fundraiser(fundraiserId);
        if (fundraiser.getBeneficiaryId().equals(viewerId)) {
            throw new NoSuchElementException("Fundraiser not found: " + fundraiserId);
        }
        return fundraiser;
    }

    public List<Contribution> contributions(UUID fundraiserId) {
        return contributions.findByFundraiserIdOrderByPaidAtAsc(fundraiserId);
    }

    public Fundraiser cancel(UUID ownerId, UUID fundraiserId) {
        return transaction.execute(status -> {
            var fundraiser = fundraiser(fundraiserId);
            if (!fundraiser.getOwnerId().equals(ownerId)) {
                throw new AccessDeniedException("Only the organizer can cancel the fundraiser");
            }
            if (!fundraiser.isActive()) {
                throw new IllegalStateException("Fundraiser is already closed");
            }
            fundraiser.cancel(Instant.now(clock));
            publishClosed(fundraiser);
            return fundraiser;
        });
    }

    public void applyPayment(PaymentSucceeded payment) {
        transaction.executeWithoutResult(status -> {
            if (processedEvents.existsById(payment.eventId())) {
                return;
            }
            processedEvents.save(new ProcessedEvent(payment.eventId(), Instant.now(clock)));
            var fundraiser = fundraisers.findById(payment.fundraiserId()).orElse(null);
            if (fundraiser == null || !fundraiser.isActive()) {
                return;
            }
            contributions.save(new Contribution(payment.paymentId(), fundraiser.getId(), payment.payerId(),
                    payment.amount(), payment.paidAt()));
            if (fundraiser.contribute(payment.amount(), Instant.now(clock))) {
                publishClosed(fundraiser);
            }
        });
    }

    private void publishClosed(Fundraiser fundraiser) {
        var eventId = UUID.randomUUID();
        outbox.fundraiserClosed(eventId, new FundraiserClosed(eventId, fundraiser.getId(), fundraiser.getStatus(),
                fundraiser.getCollectedAmount(), fundraiser.getClosedAt()));
    }

    private Fundraiser fundraiser(UUID id) {
        return fundraisers.findById(id).orElseThrow(() -> new NoSuchElementException("Fundraiser not found: " + id));
    }
}
