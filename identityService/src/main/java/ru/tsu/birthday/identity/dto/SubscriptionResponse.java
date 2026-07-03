package ru.tsu.birthday.identity.dto;

import ru.tsu.birthday.identity.entity.Subscription;
import java.util.UUID;

public record SubscriptionResponse(
        UUID subscriberId,
        Subscription.TargetType targetType,
        UUID targetId
) {}
