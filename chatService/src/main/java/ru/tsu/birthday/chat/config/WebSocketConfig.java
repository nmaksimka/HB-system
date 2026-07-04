package ru.tsu.birthday.chat.config;

import java.time.Clock;
import java.time.Instant;
import java.util.UUID;
import java.util.regex.Pattern;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;
import ru.tsu.birthday.chat.security.CurrentUser;
import ru.tsu.birthday.chat.service.ChatService;

@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {
    private static final Pattern CHAT_TOPIC = Pattern.compile("/topic/chat/([0-9a-fA-F-]{36})");

    private final JwtDecoder jwtDecoder;
    private final CurrentUser currentUser;
    private final ChatService chatService;
    private final Clock clock;

    private final String[] allowedOrigins;

    public WebSocketConfig(JwtDecoder jwtDecoder, CurrentUser currentUser, ChatService chatService, Clock clock,
                           @Value("${chat.allowed-origins}") String[] allowedOrigins) {
        this.jwtDecoder = jwtDecoder;
        this.currentUser = currentUser;
        this.chatService = chatService;
        this.clock = clock;
        this.allowedOrigins = allowedOrigins;
    }

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        registry.enableSimpleBroker("/topic");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws").setAllowedOrigins(allowedOrigins);
    }

    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        registration.interceptors(new ChannelInterceptor() {
            @Override
            public Message<?> preSend(Message<?> message, MessageChannel channel) {
                var accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
                if (accessor == null || accessor.getCommand() == null) {
                    return message;
                }
                switch (accessor.getCommand()) {
                    case CONNECT -> accessor.setUser(authenticate(accessor));
                    case SUBSCRIBE -> authorizeSubscription(accessor);
                    case SEND -> throw new AccessDeniedException("Send messages through the REST API");
                    default -> { }
                }
                return message;
            }
        });
    }

    private JwtAuthenticationToken authenticate(StompHeaderAccessor accessor) {
        var authorization = accessor.getFirstNativeHeader("Authorization");
        if (authorization == null || !authorization.startsWith("Bearer ")) {
            throw new AccessDeniedException("Bearer token is required to connect");
        }
        return new JwtAuthenticationToken(jwtDecoder.decode(authorization.substring(7)));
    }

    private void authorizeSubscription(StompHeaderAccessor accessor) {
        if (!(accessor.getUser() instanceof JwtAuthenticationToken token)) {
            throw new AccessDeniedException("Connect with a token before subscribing");
        }
        var expiresAt = token.getToken().getExpiresAt();
        if (expiresAt != null && expiresAt.isBefore(Instant.now(clock))) {
            throw new AccessDeniedException("Token has expired, reconnect");
        }
        var destination = accessor.getDestination();
        var matcher = destination == null ? null : CHAT_TOPIC.matcher(destination);
        if (matcher == null || !matcher.matches()) {
            throw new AccessDeniedException("Unknown destination");
        }
        chatService.requireAccess(UUID.fromString(matcher.group(1)), currentUser.id(token.getToken()));
    }
}
