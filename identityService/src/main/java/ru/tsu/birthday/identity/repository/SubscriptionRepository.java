package ru.tsu.birthday.identity.repository;

import java.util.*;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import ru.tsu.birthday.identity.entity.Subscription;

public interface SubscriptionRepository extends JpaRepository<Subscription, UUID> {
    List<Subscription> findBySubscriberId(UUID subscriberId);

    Optional<Subscription> findBySubscriberIdAndTargetTypeAndTargetId(UUID subscriberId,
                                                                       Subscription.TargetType targetType,
                                                                       UUID targetId);

    void deleteBySubscriberIdAndTargetTypeAndTargetId(UUID subscriberId, Subscription.TargetType targetType,
                                                       UUID targetId);

    @Query("select distinct s.subscriberId from Subscription s")
    List<UUID> findDistinctSubscriberIds();
}
