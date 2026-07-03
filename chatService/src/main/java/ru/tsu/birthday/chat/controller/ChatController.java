package ru.tsu.birthday.chat.controller;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.messaging.handler.annotation.*;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.web.bind.annotation.*;
import ru.tsu.birthday.chat.service.ChatService;
import ru.tsu.birthday.chat.dto.*;
import ru.tsu.birthday.chat.mapper.ChatMessageMapper;
import java.time.Instant;
import java.util.*;

@RestController
@RequestMapping("/api/chat")
public class ChatController {
    private final ChatService service;
    private final SimpMessagingTemplate messaging;
    private final ChatMessageMapper mapper;

    public ChatController(ChatService service, SimpMessagingTemplate messaging, ChatMessageMapper mapper) {
        this.service = service;
        this.messaging = messaging;
        this.mapper = mapper;
    }

    @GetMapping("/users/{subjectUserId}/messages")
    List<MessageResponse> history(@PathVariable UUID subjectUserId, @RequestParam UUID viewerId) {
        return service.history(subjectUserId, viewerId).stream().map(mapper::toResponse).toList();
    }

    @PostMapping("/users/{subjectUserId}/messages")
    @ResponseStatus(HttpStatus.CREATED)
    MessageResponse send(@PathVariable UUID subjectUserId, @Valid @RequestBody SendMessageRequest request) {
        return saveAndBroadcast(subjectUserId, request);
    }

    @MessageMapping("/chat/{subjectUserId}")
    public void websocketSend(@DestinationVariable UUID subjectUserId, @Valid SendMessageRequest request) {
        saveAndBroadcast(subjectUserId, request);
    }

    private MessageResponse saveAndBroadcast(UUID subjectUserId, SendMessageRequest request) {
        var view = mapper.toResponse(service.send(subjectUserId, request.authorId(), request.text()));
        messaging.convertAndSend("/topic/chat/" + subjectUserId, view);
        return view;
    }
}
