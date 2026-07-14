package ru.tsu.birthday.bank.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "payments",
       uniqueConstraints = @UniqueConstraint(columnNames = {"payer_id", "idempotency_key"}))
public class Payment {
    @Id private UUID id;
    @Column(nullable = false) private UUID fundraiserId;
    @Column(name = "payer_id", nullable = false) private UUID payerId;
    @Column(nullable = false, precision = 12, scale = 2) private BigDecimal amount;
    @Column(name = "idempotency_key", nullable = false, length = 64) private String idempotencyKey;
    @Column(nullable = false) private Instant paidAt;
    private Instant refundedAt;

    protected Payment() {}

    public Payment(UUID id, UUID fundraiserId, UUID payerId, BigDecimal amount, String idempotencyKey,
                   Instant paidAt) {
        this.id = id;
        this.fundraiserId = fundraiserId;
        this.payerId = payerId;
        this.amount = amount;
        this.idempotencyKey = idempotencyKey;
        this.paidAt = paidAt;
    }

    public void refund(Instant timestamp) {
        if (refundedAt == null) {
            refundedAt = timestamp;
        }
    }

    public UUID getId() { return id; }
    public Instant getRefundedAt() { return refundedAt; }
    public UUID getFundraiserId() { return fundraiserId; }
    public UUID getPayerId() { return payerId; }
    public BigDecimal getAmount() { return amount; }
    public String getIdempotencyKey() { return idempotencyKey; }
    public Instant getPaidAt() { return paidAt; }
}
