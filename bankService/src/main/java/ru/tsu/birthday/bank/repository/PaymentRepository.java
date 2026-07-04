package ru.tsu.birthday.bank.repository;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import ru.tsu.birthday.bank.entity.Payment;

public interface PaymentRepository extends JpaRepository<Payment, UUID> {
    Optional<Payment> findByPayerIdAndIdempotencyKey(UUID payerId, String idempotencyKey);

    @Query("""
            select coalesce(sum(p.amount), 0) from Payment p
            where p.fundraiserId = :fundraiserId and p.refundedAt is null
            """)
    BigDecimal totalPaid(UUID fundraiserId);
}
