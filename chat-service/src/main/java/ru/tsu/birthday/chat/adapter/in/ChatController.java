package ru.tsu.birthday.chat.adapter.in;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.springframework.http.HttpStatus;
import org.springframework.messaging.handler.annotation.*;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.web.bind.annotation.*;
import ru.tsu.birthday.chat.application.ChatService;
import ru.tsu.birthday.chat.domain.ChatMessage;
import java.time.Instant;
import java.util.*;

@RestController
@RequestMapping("/api/chat")
public class ChatController {
    private final ChatService service;
    private final SimpMessagingTemplate messaging;

    public ChatController(ChatService service, SimpMessagingTemplate messaging) {
        this.service = service; this.messaging = messaging;
    }

    @GetMapping("/users/{subjectUserId}/messages")
    List<MessageView> history(@PathVariable UUID subjectUserId, @RequestParam UUID viewerId) {
        return service.history(subjectUserId, viewerId).stream().map(MessageView::from).toList();
    }

    @PostMapping("/users/{subjectUserId}/messages")
    @ResponseStatus(HttpStatus.CREATED)
    MessageView send(@PathVariable UUID subjectUserId, @Valid @RequestBody SendMessage request) {
        return saveAndBroadcast(subjectUserId, request);
    }

    @MessageMapping("/chat/{subjectUserId}")
    public void websocketSend(@DestinationVariable UUID subjectUserId, @Valid SendMessage request) {
        saveAndBroadcast(subjectUserId, request);
    }

    private MessageView saveAndBroadcast(UUID subjectUserId, SendMessage request) {
        var view = MessageView.from(service.send(subjectUserId, request.authorId(), request.text()));
        messaging.convertAndSend("/topic/chat/" + subjectUserId, view);
        return view;
    }

    public record SendMessage(@NotNull UUID authorId, @NotBlank @Size(max = 2000) String text) {}
    public record MessageView(UUID id, UUID subjectUserId, UUID authorId, String text, Instant createdAt) {
        static MessageView from(ChatMessage m) {
            return new MessageView(m.getId(), m.getSubjectUserId(), m.getAuthorId(), m.getText(), m.getCreatedAt());
        }
    }
}
