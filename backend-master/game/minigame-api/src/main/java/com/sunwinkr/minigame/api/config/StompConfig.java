package com.sunwinkr.minigame.api.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

/**
 * STOMP broker config. Plan §5.4.
 *
 * <p>Topic layout (per plan §5.4):
 * <ul>
 *   <li>{@code /topic/taixiu/{moneyType}/tick}        — per-second
 *       censored snapshot (pots + remainTime; NO dice pre-reveal)</li>
 *   <li>{@code /topic/taixiu/{moneyType}/reveal}      — dice payload on
 *       REVEALED transition</li>
 *   <li>{@code /topic/taixiu/{moneyType}/pot}         — pot deltas on bet</li>
 *   <li>{@code /topic/taixiu/{moneyType}/round-start} — new round event</li>
 * </ul>
 *
 * <p>Simple broker only — no STOMP relay to RabbitMQ. The legacy game
 * already publishes to {@code queue_taixiu} for downstream history
 * persistence; STOMP topics here are a separate live-push channel for
 * the future web client.
 */
@Configuration
@EnableWebSocketMessageBroker
public class StompConfig implements WebSocketMessageBrokerConfigurer {

    /** STOMP endpoint path. */
    public static final String STOMP_ENDPOINT = "/ws/minigame";

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
