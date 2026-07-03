package ru.tsu.birthday.chat.application;

import org.junit.jupiter.api.Test;
import java.util.UUID;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.mockito.Mockito.mock;

class ChatServiceTest {
    @Test
    void preventsBirthdayPersonFromWritingToSecretChat() {
        var userId = UUID.randomUUID();
        var service = new ChatService(mock(ChatMessageRepository.class));
        assertThatIllegalArgumentException()
                .isThrownBy(() -> service.send(userId, userId, "Я всё вижу"));
    }

    @Test
    void preventsBirthdayPersonFromReadingSecretChat() {
        var userId = UUID.randomUUID();
        var service = new ChatService(mock(ChatMessageRepository.class));
        assertThatIllegalArgumentException()
                .isThrownBy(() -> service.history(userId, userId));
    }
}
