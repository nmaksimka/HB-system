package ru.tsu.birthday.bank.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "collections")
public class Collection {
    @Id private UUID fundraiserId;
    @Column(nullable = false) private UUID beneficiaryId;
    @Column(nullable = false, precision = 12, scale = 2) private BigDecimal targetAmount;
    @Column(nullable = false) private boolean open;
    @Column(nullable = false, updatable = false) private Instant openedAt;
    private Instant closedAt;

    protected Collection() {}

    public Collection(UUID fundraiserId, UUID beneficiaryId, BigDecimal targetAmount, Instant openedAt) {
        this.fundraiserId = fundraiserId;
        this.beneficiaryId = beneficiaryId;
        this.targetAmount = targetAmount;
        this.open = true;
        this.openedAt = openedAt;
    }

    public void close(Instant timestamp) {
        open = false;
        closedAt = timestamp;
    }

    public UUID getFundraiserId() { return fundraiserId; }
    public UUID getBeneficiaryId() { return beneficiaryId; }
    public BigDecimal getTargetAmount() { return targetAmount; }
    public boolean isOpen() { return open; }
    public Instant getOpenedAt() { return openedAt; }
    public Instant getClosedAt() { return closedAt; }
}
