package ru.tsu.birthday.chat.service;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;
import ru.tsu.birthday.chat.client.IdentityClient;
import ru.tsu.birthday.chat.entity.ChatMessage;
import ru.tsu.birthday.chat.repository.ChatMessageRepository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ChatServiceTest {
    private final ChatMessageRepository repository = mock(ChatMessageRepository.class);
    private final IdentityClient identityClient = mock(IdentityClient.class);
    private final Clock clock = Clock.fixed(Instant.parse("2026-01-01T10:00:00Z"), ZoneOffset.UTC);
    private final ChatService service = new ChatService(repository, identityClient, clock);

    @Test
    void preventsBirthdayPersonFromWritingToSecretChat() {
        var userId = UUID.randomUUID();

        assertThatThrownBy(() -> service.send(userId, userId, "Я всё вижу"))
                .isInstanceOf(AccessDeniedException.class);
        verify(repository, never()).save(any());
    }

    @Test
    void preventsBirthdayPersonFromReadingSecretChat() {
        var userId = UUID.randomUUID();

        assertThatThrownBy(() -> service.history(userId, userId))
                .isInstanceOf(AccessDeniedException.class);
        verify(repository, never()).findTop100BySubjectUserIdOrderByCreatedAtDesc(any());
    }

    @Test
    void rejectsWritingWithoutAccessToSubject() {
        var subjectId = UUID.randomUUID();
        var authorId = UUID.randomUUID();
        when(identityClient.canDiscuss(subjectId, authorId)).thenReturn(false);

        assertThatThrownBy(() -> service.send(subjectId, authorId, "Привет"))
                .isInstanceOf(AccessDeniedException.class);
        verify(repository, never()).save(any());
    }

    @Test
    void rejectsReadingWithoutAccessToSubject() {
        var subjectId = UUID.randomUUID();
        var viewerId = UUID.randomUUID();
        when(identityClient.canDiscuss(subjectId, viewerId)).thenReturn(false);

        assertThatThrownBy(() -> service.history(subjectId, viewerId))
                .isInstanceOf(AccessDeniedException.class);
        verify(repository, never()).findTop100BySubjectUserIdOrderByCreatedAtDesc(any());
    }

    @Test
    void trimsTextAndSavesMessageWithCurrentTime() {
        var subjectId = UUID.randomUUID();
        var authorId = UUID.randomUUID();
        when(identityClient.canDiscuss(subjectId, authorId)).thenReturn(true);
        when(repository.save(any(ChatMessage.class))).thenAnswer(call -> call.getArgument(0));

        var message = service.send(subjectId, authorId, "  Дарим книгу  ");

        assertThat(message.getText()).isEqualTo("Дарим книгу");
        assertThat(message.getSubjectUserId()).isEqualTo(subjectId);
        assertThat(message.getAuthorId()).isEqualTo(authorId);
        assertThat(message.getCreatedAt()).isEqualTo(clock.instant());
    }

    @Test
    void returnsHistoryInChronologicalOrder() {
        var subjectId = UUID.randomUUID();
        var viewerId = UUID.randomUUID();
        var older = new ChatMessage(UUID.randomUUID(), subjectId, viewerId, "first",
                Instant.parse("2026-01-01T09:00:00Z"));
        var newer = new ChatMessage(UUID.randomUUID(), subjectId, viewerId, "second",
                Instant.parse("2026-01-01T09:30:00Z"));
        when(identityClient.canDiscuss(subjectId, viewerId)).thenReturn(true);
        when(repository.findTop100BySubjectUserIdOrderByCreatedAtDesc(subjectId)).thenReturn(List.of(newer, older));

        assertThat(service.history(subjectId, viewerId)).containsExactly(older, newer);
    }

    @Test
    void returnsEmptyHistoryForNewChat() {
        var subjectId = UUID.randomUUID();
        var viewerId = UUID.randomUUID();
        when(identityClient.canDiscuss(subjectId, viewerId)).thenReturn(true);
        when(repository.findTop100BySubjectUserIdOrderByCreatedAtDesc(subjectId)).thenReturn(List.of());

        assertThat(service.history(subjectId, viewerId)).isEmpty();
    }
}
