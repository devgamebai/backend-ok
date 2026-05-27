package com.sunwinkr.lottery.api.push;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.stereotype.Controller;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

/**
 * STOMP chat relay — accepts messages on {@code /app/chat} and rebroadcasts
 * to {@code /topic/lottery/xsmb/chat}.
 *
 * <p>Plan §5.4 — chat + announce ONLY. Chat payload is the player's
 * free-form text; the broker re-emits with a server-stamped timestamp.
 *
 * <p>Wire shape: {@code {user, msg, ts}} — caller supplies {@code user}
 * and {@code msg}, server stamps {@code ts}.
 *
 * <p>No persistence — chat is ephemeral. Moderation lives outside this
 * module (existing admin chat tools).
 */
@Controller
public class ChatRelayController {

    private static final Logger LOG = LoggerFactory.getLogger(ChatRelayController.class);

    /** Server-side max chat length — clamped silently. */
    public static final int MAX_MSG_LEN = 280;

    /** STOMP topic the broker fans out to. */
    public static final String TOPIC_CHAT = "/topic/lottery/xsmb/chat";

    /** Client SEND destination — {@code /app} prefix per StompConfig. */
    public static final String APP_CHAT = "/chat";

    @MessageMapping(APP_CHAT)
    @SendTo(TOPIC_CHAT)
    public Map<String, Object> relay(Map<String, Object> incoming) {
        String user = incoming == null ? "" : String.valueOf(incoming.getOrDefault("user", ""));
        String msg = incoming == null ? "" : String.valueOf(incoming.getOrDefault("msg", ""));
        if (msg.length() > MAX_MSG_LEN) {
            msg = msg.substring(0, MAX_MSG_LEN);
        }
        Map<String, Object> out = new HashMap<>();
        out.put("user", user);
        out.put("msg", msg);
        out.put("ts", Instant.now().toEpochMilli());
        return out;
    }
}
