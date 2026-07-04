package ru.tsu.birthday.fundraiser.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "fundraisers")
public class Fundraiser {
    @Id private UUID id;
    @Column(nullable = false) private UUID ownerId;
    @Column(nullable = false) private UUID beneficiaryId;
    @Column(nullable = false) private UUID wishId;
    @Column(nullable = false, length = 160) private String title;
    @Column(nullable = false, precision = 12, scale = 2) private BigDecimal targetAmount;
    @Column(nullable = false, precision = 12, scale = 2) private BigDecimal collectedAmount;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20) private FundraiserStatus status;
    @Column(nullable = false, updatable = false) private Instant createdAt;
    private Instant closedAt;
    @Version private long version;

    protected Fundraiser() {}

    public Fundraiser(UUID id, UUID ownerId, UUID beneficiaryId, UUID wishId, String title,
                      BigDecimal targetAmount, Instant createdAt) {
        this.id = id;
        this.ownerId = ownerId;
        this.beneficiaryId = beneficiaryId;
        this.wishId = wishId;
        this.title = title;
        this.targetAmount = targetAmount;
        this.collectedAmount = BigDecimal.ZERO;
        this.status = FundraiserStatus.ACTIVE;
        this.createdAt = createdAt;
    }

    public boolean isActive() { return status == FundraiserStatus.ACTIVE; }

    public boolean contribute(BigDecimal amount, Instant timestamp) {
        collectedAmount = collectedAmount.add(amount);
        if (collectedAmount.compareTo(targetAmount) >= 0) {
            close(FundraiserStatus.COMPLETED, timestamp);
            return true;
        }
        return false;
    }

    public void cancel(Instant timestamp) { close(FundraiserStatus.CANCELLED, timestamp); }

    private void close(FundraiserStatus finalStatus, Instant timestamp) {
        status = finalStatus;
        closedAt = timestamp;
    }

    public UUID getId() { return id; }
    public UUID getOwnerId() { return ownerId; }
    public UUID getBeneficiaryId() { return beneficiaryId; }
    public UUID getWishId() { return wishId; }
    public String getTitle() { return title; }
    public BigDecimal getTargetAmount() { return targetAmount; }
    public BigDecimal getCollectedAmount() { return collectedAmount; }
    public FundraiserStatus getStatus() { return status; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getClosedAt() { return closedAt; }
}
