package com.sunwinkr.lottery.api.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

/**
 * STOMP broker config. Plan §5.4 (chat + announce ONLY — NO result frames).
 *
 * <p>Topic layout (per plan §5.4):
 * <ul>
 *   <li>{@code /topic/lottery/xsmb/chat}     — chat messages relayed
 *       (client → /app/chat → /topic/lottery/xsmb/chat). Payload:
 *       {@code {user, msg, ts}}.</li>
 *   <li>{@code /topic/lottery/xsmb/announce} — one-shot on
 *       {@code markDraySettled}. Payload: {@code {type:"settled", date}}.
 *       NO result fields (ĐB / G1..G7). Result data NEVER traverses WS.
 *       Client receives announce → triggers REST GET /result/{date}.</li>
 *   <li>{@code /topic/lottery/xsmb/lock}     — one-shot at 18:10 VN lock.
 *       Payload: {@code {type:"locked", lockTime}}.</li>
 * </ul>
 *
 * <p>There are NO 1Hz tick frames. Lottery is daily — there is no
 * countdown to broadcast. The legacy game already has no STOMP push for
 * lottery; this module adds chat + announce only.
 *
 * <p>Simple broker only — no STOMP relay to RabbitMQ. The legacy game
 * already publishes to MySQL/Mongo for downstream persistence; STOMP
 * topics here are a separate live-push channel for the future web client.
 */
@Configuration
@EnableWebSocketMessageBroker
public class StompConfig implements WebSocketMessageBrokerConfigurer {

    /** STOMP endpoint path. */
    public static final String STOMP_ENDPOINT = "/ws/lottery";

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        registry.enableSimpleBroker("/topic");
        registry.setApplicationDestinationPrefixes("/app");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint(STOMP_ENDPOINT)
            .setAllowedOriginPatterns("*");
        // SockJS fallback intentionally omitted — Cocos client uses raw
        // WebSocket transport.
    }
}
