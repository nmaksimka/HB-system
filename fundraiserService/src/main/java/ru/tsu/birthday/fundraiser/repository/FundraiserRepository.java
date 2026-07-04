package ru.tsu.birthday.fundraiser.repository;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import ru.tsu.birthday.fundraiser.entity.Fundraiser;
import ru.tsu.birthday.fundraiser.entity.FundraiserStatus;

public interface FundraiserRepository extends JpaRepository<Fundraiser, UUID> {
    List<Fundraiser> findByBeneficiaryIdOrderByCreatedAtDesc(UUID beneficiaryId);
    boolean existsByWishIdAndStatus(UUID wishId, FundraiserStatus status);
}
