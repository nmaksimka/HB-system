package ru.tsu.birthday.chat.controller;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.messaging.handler.annotation.*;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import ru.tsu.birthday.chat.service.ChatService;
import ru.tsu.birthday.chat.dto.*;
import ru.tsu.birthday.chat.mapper.ChatMessageMapper;
import ru.tsu.birthday.chat.security.CurrentUser;
import java.security.Principal;
import java.util.*;

@RestController
@RequestMapping("/api/chat")
public class ChatController {
    private final ChatService service;
    private final SimpMessagingTemplate messaging;
    private final ChatMessageMapper mapper;
    private final CurrentUser currentUser;

    public ChatController(ChatService service, SimpMessagingTemplate messaging, ChatMessageMapper mapper,
                          CurrentUser currentUser) {
        this.service = service;
        this.messaging = messaging;
        this.mapper = mapper;
        this.currentUser = currentUser;
    }

    @GetMapping("/users/{subjectUserId}/messages")
    List<MessageResponse> history(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID subjectUserId) {
        return service.history(subjectUserId, currentUser.id(jwt)).stream().map(mapper::toResponse).toList();
    }

    @PostMapping("/users/{subjectUserId}/messages")
    @ResponseStatus(HttpStatus.CREATED)
    MessageResponse send(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID subjectUserId,
                         @Valid @RequestBody SendMessageRequest request) {
        return saveAndBroadcast(subjectUserId, currentUser.id(jwt), request);
    }

    @MessageMapping("/chat/{subjectUserId}")
    public void websocketSend(Principal principal, @DestinationVariable UUID subjectUserId,
                              @Valid SendMessageRequest request) {
        saveAndBroadcast(subjectUserId, currentUser.id(principal), request);
    }

    private MessageResponse saveAndBroadcast(UUID subjectUserId, UUID authorId, SendMessageRequest request) {
        var view = mapper.toResponse(service.send(subjectUserId, authorId, request.text()));
        messaging.convertAndSend("/topic/chat/" + subjectUserId, view);
        return view;
    }
}
