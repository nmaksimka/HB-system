package ru.tsu.birthday.identity.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.tsu.birthday.identity.entity.Subscription;
import java.util.*;

public interface SubscriptionRepository extends JpaRepository<Subscription, UUID> {
    List<Subscription> findBySubscriberId(UUID subscriberId);
}
