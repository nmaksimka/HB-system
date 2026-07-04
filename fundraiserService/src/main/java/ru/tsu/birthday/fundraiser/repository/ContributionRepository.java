package ru.tsu.birthday.fundraiser.repository;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import ru.tsu.birthday.fundraiser.entity.Contribution;

public interface ContributionRepository extends JpaRepository<Contribution, UUID> {
    List<Contribution> findByFundraiserIdOrderByPaidAtAsc(UUID fundraiserId);
}
