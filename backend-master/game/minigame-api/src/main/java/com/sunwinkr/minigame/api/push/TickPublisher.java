package com.sunwinkr.minigame.api.push;

import com.sunwinkr.minigame.api.dto.StateDto;
import com.sunwinkr.minigame.engine.core.TaiXiuRound;
import com.sunwinkr.minigame.engine.snapshot.TaiXiuSnapshot;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * STOMP topic publisher. Plan §5.4.
 *
 * <p>Per-second tick broadcast — censored snapshot (no dice pre-reveal).
 * The {@link #publishReveal()} / {@link #publishPotDelta()} /
 * {@link #publishRoundStart()} methods are called by
 * {@code TaiXiuModuleBridge} on engine events.
 *
 * <p>Default-OFF: the {@link #MINIGAME_API_PUSH_ENABLED} env flag (set by
 * deploy) gates whether broadcasts actually fire. When false the
 * scheduled tick is a no-op so the legacy BitZero broadcasts remain
 * authoritative during shadow/cutover.
 */
@Component
public class TickPublisher {

    private static final Logger LOG = LoggerFactory.getLogger(TickPublisher.class);

    /** Env flag — set to "1" to enable STOMP broadcast. Default OFF. */
    public static final String FLAG_ENV = "MINIGAME_API_PUSH_ENABLED";

    private final TaiXiuRound round;
    private final SimpMessagingTemplate broker;

    public TickPublisher(TaiXiuRound round, SimpMessagingTemplate broker) {
        this.round = round;
        this.broker = broker;
    }

    /** 1Hz tick broadcast. Topic: {@code /topic/taixiu/{moneyType}/tick}. */
    @Scheduled(fixedRate = 1000L)
    public void onTick() {
        if (!isEnabled()) {
            return;
        }
        try {
            TaiXiuSnapshot snap = round.snapshotForClient("");
            StateDto dto = StateDto.fromSnapshot(snap);
            // Both moneyTypes share a single round in PR-4; per-money-type
            // pot fields live in StateDto.potTai/potXiu.
            broker.convertAndSend("/topic/taixiu/1/tick", dto);
            broker.convertAndSend("/topic/taixiu/0/tick", dto);
        } catch (Throwable t) {
            LOG.warn("TickPublisher.onTick failed", t);
        }
    }

    /** Fired by bridge on REVEALED transition. */
    public void publishReveal(TaiXiuSnapshot snap) {
        if (!isEnabled() || snap == null) {
            return;
        }
        StateDto dto = StateDto.fromSnapshot(snap);
        broker.convertAndSend("/topic/taixiu/1/reveal", dto);
        broker.convertAndSend("/topic/taixiu/0/reveal", dto);
    }

    /** Fired by bridge on pot mutation (bet accepted). */
    public void publishPotDelta(short moneyType, long potTai, long potXiu) {
        if (!isEnabled()) {
            return;
        }
        java.util.Map<String, Object> m = new java.util.HashMap<>();
        m.put("potTai", potTai);
        m.put("potXiu", potXiu);
        broker.convertAndSend("/topic/taixiu/" + moneyType + "/pot", m);
    }

    /** Fired by bridge on new round. */
    public void publishRoundStart(long referenceId) {
        if (!isEnabled()) {
            return;
        }
        java.util.Map<String, Object> m = new java.util.HashMap<>();
        m.put("referenceId", referenceId);
        broker.convertAndSend("/topic/taixiu/1/round-start", m);
        broker.convertAndSend("/topic/taixiu/0/round-start", m);
    }

    static boolean isEnabled() {
        String v = System.getenv(FLAG_ENV);
        return v != null && (v.equals("1") || v.equalsIgnoreCase("true"));
    }
}
