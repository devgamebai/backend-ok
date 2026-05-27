package com.sunwinkr.lottery.api.push;

import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit test for {@link ChatRelayController#relay} — verifies the
 * client→relay→broker round-trip payload shape (user, msg, ts) and
 * the server-side 280-char clamp.
 *
 * <p>Plan §5.4 chat channel.
 */
class StompChatTest {

    @Test
    void chat_relayPreservesUserAndMessage_addsServerTs() {
        ChatRelayController c = new ChatRelayController();
        Map<String, Object> incoming = new HashMap<>();
        incoming.put("user", "player1");
        incoming.put("msg", "hello there");

        Map<String, Object> out = c.relay(incoming);

        assertThat(out).containsEntry("user", "player1");
        assertThat(out).containsEntry("msg", "hello there");
        assertThat(out).containsKey("ts");
        assertThat((Long) out.get("ts")).isPositive();
    }

    @Test
    void chat_relayClampsLongMessages_to280Chars() {
        ChatRelayController c = new ChatRelayController();
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 500; i++) sb.append("x");
        Map<String, Object> incoming = new HashMap<>();
        incoming.put("user", "player1");
        incoming.put("msg", sb.toString());

        Map<String, Object> out = c.relay(incoming);

        assertThat(((String) out.get("msg")).length()).isEqualTo(ChatRelayController.MAX_MSG_LEN);
    }
}
