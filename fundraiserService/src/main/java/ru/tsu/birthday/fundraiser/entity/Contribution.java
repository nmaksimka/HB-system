package ru.tsu.birthday.fundraiser.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "contributions")
public class Contribution {
    @Id private UUID paymentId;
    @Column(nullable = false) private UUID fundraiserId;
    @Column(nullable = false) private UUID payerId;
    @Column(nullable = false, precision = 12, scale = 2) private BigDecimal amount;
    @Column(nullable = false) private Instant paidAt;

    protected Contribution() {}

    public Contribution(UUID paymentId, UUID fundraiserId, UUID payerId, BigDecimal amount, Instant paidAt) {
        this.paymentId = paymentId;
        this.fundraiserId = fundraiserId;
        this.payerId = payerId;
        this.amount = amount;
        this.paidAt = paidAt;
    }

    public UUID getPaymentId() { return paymentId; }
    public UUID getFundraiserId() { return fundraiserId; }
    public UUID getPayerId() { return payerId; }
    public BigDecimal getAmount() { return amount; }
    public Instant getPaidAt() { return paidAt; }
}
