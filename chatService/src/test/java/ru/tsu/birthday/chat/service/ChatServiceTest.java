package ru.tsu.birthday.chat.service;

import ru.tsu.birthday.chat.repository.ChatMessageRepository;

import org.junit.jupiter.api.Test;
import java.util.UUID;
import java.time.Clock;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.mockito.Mockito.mock;

class ChatServiceTest {
    @Test
    void preventsBirthdayPersonFromWritingToSecretChat() {
        var userId = UUID.randomUUID();
        var service = new ChatService(mock(ChatMessageRepository.class), Clock.systemUTC());
        assertThatIllegalArgumentException()
                .isThrownBy(() -> service.send(userId, userId, "Я всё вижу"));
    }

    @Test
    void preventsBirthdayPersonFromReadingSecretChat() {
        var userId = UUID.randomUUID();
        var service = new ChatService(mock(ChatMessageRepository.class), Clock.systemUTC());
        assertThatIllegalArgumentException()
                .isThrownBy(() -> service.history(userId, userId));
    }
}
