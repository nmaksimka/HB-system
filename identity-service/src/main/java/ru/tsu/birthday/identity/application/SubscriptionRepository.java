package ru.tsu.birthday.identity.application;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.tsu.birthday.identity.domain.Subscription;
import java.util.*;

public interface SubscriptionRepository extends JpaRepository<Subscription, UUID> {
    List<Subscription> findBySubscriberId(UUID subscriberId);
}
