package ru.tsu.birthday.gift.mapper;

import java.util.UUID;
import org.springframework.stereotype.Component;
import ru.tsu.birthday.gift.dto.WishResponse;
import ru.tsu.birthday.gift.entity.Wish;

@Component
public class WishMapper {
    public WishResponse toResponse(Wish wish, UUID viewerId) {
        var ownerIsViewing = wish.getUserId().equals(viewerId);
        return new WishResponse(
                wish.getId(),
                wish.getUserId(),
                wish.getTitle(),
                wish.getDescription(),
                wish.getLink(),
                wish.getPriority(),
                !ownerIsViewing && wish.isReserved(),
                !ownerIsViewing && wish.isReservedBy(viewerId)
        );
    }
}
