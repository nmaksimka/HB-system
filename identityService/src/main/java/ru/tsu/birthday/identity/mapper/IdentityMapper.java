package ru.tsu.birthday.identity.mapper;

import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Component;
import ru.tsu.birthday.identity.dto.*;
import ru.tsu.birthday.identity.entity.*;

@Component
public class IdentityMapper {
    public UserResponse toResponse(User user) {
        return new UserResponse(user.getId(), user.getName(), user.getBirthDate());
    }

    public GroupResponse toResponse(FriendGroup group, List<UUID> memberIds) {
        return new GroupResponse(group.getId(), group.getName(), group.getOwnerId(), memberIds);
    }

    public SubscriptionResponse toResponse(Subscription subscription) {
        return new SubscriptionResponse(subscription.getTargetType(), subscription.getTargetId());
    }
}
