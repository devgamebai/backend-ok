package com.sunwinkr.minigame.api.push;

import com.sunwinkr.minigame.api.dto.sicbo.SicboStateDto;
import com.sunwinkr.minigame.engine.sicbo.core.SicboRound;
import com.sunwinkr.minigame.engine.sicbo.snapshot.SicboSnapshot;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Sicbo STOMP topic publisher. Plan §6 (analog of TaiXiu §5.4).
 *
 * <p>Topic surface (per moneyType):
 * <ul>
 *   <li>{@code /topic/sicbo/{moneyType}/tick}         — 1Hz censored snapshot</li>
 *   <li>{@code /topic/sicbo/{moneyType}/reveal}       — dice + result on REVEALED</li>
 *   <li>{@code /topic/sicbo/{moneyType}/pot}          — pot deltas on bet accepted</li>
 *   <li>{@code /topic/sicbo/{moneyType}/round-start}  — new round notification</li>
 * </ul>
 *
 * <p>Default-OFF: the {@link #FLAG_ENV} env flag gates whether broadcasts
 * actually fire. When false the scheduled tick is a no-op so the legacy
 * BitZero broadcasts remain authoritative during shadow/cutover.
 */
@Component
public class SicboTickPublisher {

    private static final Logger LOG = LoggerFactory.getLogger(SicboTickPublisher.class);

    /** Env flag — set to "1" to enable STOMP broadcast. Default OFF. */
    public static final String FLAG_ENV = "MINIGAME_API_PUSH_ENABLED";

    private final SicboRound round;
    private final SimpMessagingTemplate broker;

    public SicboTickPublisher(SicboRound round, SimpMessagingTemplate broker) {
        this.round = round;
        this.broker = broker;
    }

    /** 1Hz tick broadcast. Topic: {@code /topic/sicbo/{moneyType}/tick}. */
    @Scheduled(fixedRate = 1000L)
    public void onTick() {
        if (!isEnabled()) {
            return;
        }
        try {
            short[] dice = round.getPendingDice();
            Short d1 = dice != null && dice.length >= 3 ? dice[0] : null;
            Short d2 = dice != null && dice.length >= 3 ? dice[1] : null;
            Short d3 = dice != null && dice.length >= 3 ? dice[2] : null;
            SicboSnapshot snap = SicboSnapshot.of(
                (short) 5, (short) 1, round.getReferenceId(),
                0, round.isBetting(),
                0L, 0L, 0L, 0L, 0L, 0L,
                d1, d2, d3,
                round.getPhase());
            SicboStateDto dto = SicboStateDto.fromSnapshot(snap);
            // Both moneyTypes share a single round in PR-4.
            broker.convertAndSend("/topic/sicbo/1/tick", dto);
            broker.convertAndSend("/topic/sicbo/0/tick", dto);
        } catch (Throwable t) {
            LOG.warn("SicboTickPublisher.onTick failed", t);
        }
    }

    /** Fired by bridge on REVEALED transition. */
    public void publishReveal(SicboSnapshot snap) {
        if (!isEnabled() || snap == null) {
            return;
        }
        SicboStateDto dto = SicboStateDto.fromSnapshot(snap);
        broker.convertAndSend("/topic/sicbo/1/reveal", dto);
        broker.convertAndSend("/topic/sicbo/0/reveal", dto);
    }

    /** Fired by bridge on pot mutation (bet accepted). */
    public void publishPotDelta(short moneyType, long potTotal) {
        if (!isEnabled()) {
            return;
        }
        java.util.Map<String, Object> m = new java.util.HashMap<>();
        m.put("potTotal", potTotal);
        broker.convertAndSend("/topic/sicbo/" + moneyType + "/pot", m);
    }

    /** Fired by bridge on new round. */
    public void publishRoundStart(long referenceId) {
        if (!isEnabled()) {
            return;
        }
        java.util.Map<String, Object> m = new java.util.HashMap<>();
        m.put("referenceId", referenceId);
        broker.convertAndSend("/topic/sicbo/1/round-start", m);
        broker.convertAndSend("/topic/sicbo/0/round-start", m);
    }

    static boolean isEnabled() {
        String v = System.getenv(FLAG_ENV);
        return v != null && (v.equals("1") || v.equalsIgnoreCase("true"));
    }
}
