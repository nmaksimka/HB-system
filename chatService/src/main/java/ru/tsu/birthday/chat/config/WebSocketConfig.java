package ru.tsu.birthday.chat.config;

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
import org.springframework.web.socket.config.annotation.*;

@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {
    private final JwtDecoder jwtDecoder;

    public WebSocketConfig(JwtDecoder jwtDecoder) {
        this.jwtDecoder = jwtDecoder;
    }

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        registry.enableSimpleBroker("/topic");
        registry.setApplicationDestinationPrefixes("/app");
    }

    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        registration.interceptors(new ChannelInterceptor() {
            @Override
            public Message<?> preSend(Message<?> message, MessageChannel channel) {
                var accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
                if (accessor == null) {
                    return message;
                }
                var command = accessor.getCommand();
                var authorization = firstHeader(accessor, "authorization");
                if (authorization == null) {
                    authorization = firstHeader(accessor, "Authorization");
                }
                if (authorization != null && authorization.startsWith("Bearer ")) {
                    var jwt = jwtDecoder.decode(authorization.substring(7));
                    accessor.setUser(new JwtAuthenticationToken(jwt));
                    return message;
                }
                if (StompCommand.CONNECT.equals(command)
                        || (StompCommand.SEND.equals(command) && isApplicationDestination(accessor))) {
                    throw new AccessDeniedException("Bearer token is required for WebSocket messages");
                }
                return message;
            }
        });
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws").setAllowedOriginPatterns("*");
    }

    private static String firstHeader(StompHeaderAccessor accessor, String name) {
        var values = accessor.getNativeHeader(name);
        return values == null || values.isEmpty() ? null : values.get(0);
    }

    private static boolean isApplicationDestination(StompHeaderAccessor accessor) {
        var destination = accessor.getDestination();
        return destination != null && destination.startsWith("/app/");
    }
}
