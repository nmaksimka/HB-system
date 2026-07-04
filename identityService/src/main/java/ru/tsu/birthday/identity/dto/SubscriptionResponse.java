package ru.tsu.birthday.identity.dto;

import java.util.UUID;
import ru.tsu.birthday.identity.entity.Subscription;

public record SubscriptionResponse(Subscription.TargetType targetType, UUID targetId) {}
