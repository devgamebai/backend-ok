package com.sunwinkr.lottery.api.push;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.time.LocalDate;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

/**
 * Critical anti-leak test (plan §5.4): the {@code /announce} STOMP
 * payload MUST contain only {@code {type, date}} — NOT any result
 * fields (ĐB, G1..G7). Result data is REST-only.
 */
class StompAnnouncePayloadHasNoResultTest {

    @Test
    void announceSettled_payload_containsOnlyTypeAndDate_neverResultFields() {
        SimpMessagingTemplate broker = mock(SimpMessagingTemplate.class);
        SettleAnnouncePublisher pub = new SettleAnnouncePublisher(broker);

        pub.announceSettled(LocalDate.parse("2026-05-14"));

        ArgumentCaptor<String> destCap = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<Object> payloadCap = ArgumentCaptor.forClass(Object.class);
        verify(broker).convertAndSend(destCap.capture(), payloadCap.capture());

        assertThat(destCap.getValue()).isEqualTo("/topic/lottery/xsmb/announce");
        @SuppressWarnings("unchecked")
        Map<String, Object> payload = (Map<String, Object>) payloadCap.getValue();
        assertThat(payload).containsEntry("type", "settled");
        assertThat(payload).containsEntry("date", "2026-05-14");
        // ANTI-LEAK assertions — these fields MUST NEVER appear.
        assertThat(payload).doesNotContainKeys("DB", "ĐB", "G1", "G2", "G3", "G4", "G5", "G6", "G7",
            "result", "results", "prize", "prizes", "draw");
        // And keys are strictly the allowlist.
        assertThat(payload).containsOnlyKeys("type", "date");
    }

    @Test
    void announceLocked_payload_containsOnlyTypeAndLockTime() {
        SimpMessagingTemplate broker = mock(SimpMessagingTemplate.class);
        SettleAnnouncePublisher pub = new SettleAnnouncePublisher(broker);

        pub.announceLocked("18:10");

        ArgumentCaptor<Object> payloadCap = ArgumentCaptor.forClass(Object.class);
        verify(broker).convertAndSend(eq(SettleAnnouncePublisher.TOPIC_LOCK), payloadCap.capture());

        @SuppressWarnings("unchecked")
        Map<String, Object> payload = (Map<String, Object>) payloadCap.getValue();
        assertThat(payload).containsOnlyKeys("type", "lockTime");
        assertThat(payload).containsEntry("type", "locked");
        assertThat(payload).containsEntry("lockTime", "18:10");
    }
}
