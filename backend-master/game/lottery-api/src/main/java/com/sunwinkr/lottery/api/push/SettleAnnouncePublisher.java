package com.sunwinkr.lottery.api.push;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;

/**
 * STOMP push: settle announce + lock announce.
 *
 * <p>Plan §5.4 — chat + announce ONLY (NO result frames). Result data
 * NEVER traverses WS; the client receives {@code settled} and triggers a
 * REST {@code GET /result/{date}} to fetch the prize payload.
 *
 * <h3>Topics</h3>
 * <ul>
 *   <li>{@code /topic/lottery/xsmb/announce} — one-shot on
 *       {@code markDraySettled}. Payload {@code {type:"settled", date}}.
 *       NO ĐB / G1..G7 fields.</li>
 *   <li>{@code /topic/lottery/xsmb/lock}     — one-shot at 18:10 VN
 *       lock. Payload {@code {type:"locked", lockTime}}.</li>
 * </ul>
 */
@Component
public class SettleAnnouncePublisher {

    private static final Logger LOG = LoggerFactory.getLogger(SettleAnnouncePublisher.class);

    /** Announce topic — settled events. */
    public static final String TOPIC_ANNOUNCE = "/topic/lottery/xsmb/announce";

    /** Lock topic — 18:10 VN. */
    public static final String TOPIC_LOCK = "/topic/lottery/xsmb/lock";

    private final SimpMessagingTemplate broker;

    public SettleAnnouncePublisher(SimpMessagingTemplate broker) {
        this.broker = broker;
    }

    /**
     * One-shot on day settle complete. Payload contains ONLY
     * {@code {type:"settled", date}} — no result fields. Clients use
     * this to trigger a REST result fetch.
     */
    public void announceSettled(LocalDate vnDate) {
        if (vnDate == null) return;
        Map<String, Object> payload = new HashMap<>();
        payload.put("type", "settled");
        payload.put("date", vnDate.toString());
        try {
            broker.convertAndSend(TOPIC_ANNOUNCE, payload);
        } catch (Throwable t) {
            LOG.warn("SettleAnnouncePublisher.announceSettled failed", t);
        }
    }

    /** One-shot at 18:10 VN. Payload {@code {type:"locked", lockTime}}. */
    public void announceLocked(String lockTime) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("type", "locked");
        payload.put("lockTime", lockTime);
        try {
            broker.convertAndSend(TOPIC_LOCK, payload);
        } catch (Throwable t) {
            LOG.warn("SettleAnnouncePublisher.announceLocked failed", t);
        }
    }
}
