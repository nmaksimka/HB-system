package ru.tsu.birthday.identity.dto;

import jakarta.validation.constraints.NotNull;
import ru.tsu.birthday.identity.entity.Subscription;
import java.util.UUID;

public record CreateSubscriptionRequest(
        @NotNull Subscription.TargetType targetType,
        @NotNull UUID targetId
) {}
