package ru.tsu.birthday.identity.mapper;

import org.springframework.stereotype.Component;
import ru.tsu.birthday.identity.dto.*;
import ru.tsu.birthday.identity.entity.*;

@Component
public class IdentityMapper {
    public UserResponse toResponse(User user) {
        return new UserResponse(user.getId(), user.getName(), user.getBirthDate());
    }

    public GroupResponse toResponse(FriendGroup group) {
        return new GroupResponse(group.getId(), group.getName(), group.getOwnerId());
    }

    public SubscriptionResponse toResponse(Subscription subscription) {
        return new SubscriptionResponse(
                subscription.getSubscriberId(),
                subscription.getTargetType(),
                subscription.getTargetId()
        );
    }
}
