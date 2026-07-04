package ru.tsu.birthday.gift.mapper;

import org.springframework.stereotype.Component;
import ru.tsu.birthday.gift.dto.WishResponse;
import ru.tsu.birthday.gift.entity.Wish;

@Component
public class WishMapper {
    public WishResponse toResponse(Wish wish) {
        return new WishResponse(
                wish.getId(),
                wish.getTitle(),
                wish.getDescription(),
                wish.getLink(),
                wish.getPriority(),
                wish.getReservedBy(),
                wish.getReservedAt()
        );
    }
}
