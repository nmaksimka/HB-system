package ru.tsu.birthday.chat.mapper;

import org.springframework.stereotype.Component;
import ru.tsu.birthday.chat.dto.MessageResponse;
import ru.tsu.birthday.chat.entity.ChatMessage;

@Component
public class ChatMessageMapper {
    public MessageResponse toResponse(ChatMessage message) {
        return new MessageResponse(
                message.getId(),
                message.getSubjectUserId(),
                message.getAuthorId(),
                message.getText(),
                message.getCreatedAt()
        );
    }
}
