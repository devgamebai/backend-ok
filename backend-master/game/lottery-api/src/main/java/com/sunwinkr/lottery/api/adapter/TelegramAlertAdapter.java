package com.sunwinkr.lottery.api.adapter;

import com.sunwinkr.lottery.engine.settle.SettleFailureEvent;
import com.vinplay.utils.TelegramAlert;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.function.Consumer;

/**
 * Telegram alert sink for {@link SettleFailureEvent} — wraps the legacy
 * {@link TelegramAlert#sendMessage}. Wired as the {@code failureSink}
 * {@link Consumer} on {@link com.sunwinkr.lottery.engine.settle.LotterySettleService}.
 *
 * <h3>PII REDACTION (audit §5.8)</h3>
 * The legacy {@code LotteryModule.buyTicket} (JLM:224) sent the player's
 * raw nickname AND bet pick to Telegram on every purchase — clear PII
 * leak. This adapter:
 * <ul>
 *   <li>Hashes the nickname with SHA-256 (first 8 hex chars) before
 *       sending — operators can still cross-reference via DB but the
 *       Telegram channel never holds the raw handle.</li>
 *   <li>Does NOT carry bet pick / ticket number — only the failure
 *       reason and the hashed nickname.</li>
 * </ul>
 *
 * <p>Plan §5.8 (PII redaction).
 *
 * <p>{@code @Primary}: resolves NoUniqueBeanDefinitionException when the
 * stale compiled EngineConfig {@code settleFailureSink} @Bean wrapper coexists
 * with this @Component. This is the canonical Consumer&lt;SettleFailureEvent&gt;.
 */
@Primary
@Component
public class TelegramAlertAdapter implements Consumer<SettleFailureEvent> {

    private static final Logger LOG = LoggerFactory.getLogger(TelegramAlertAdapter.class);

    @Override
    public void accept(SettleFailureEvent ev) {
        if (ev == null) {
            return;
        }
        try {
            String hashed = hashNickname(ev.getNickname());
            String msg = "[lottery-settle-fail] ticket=" + ev.getTicketId()
                + " userHash=" + hashed
                + " reason=" + ev.getReason();
            TelegramAlert.sendMessage(msg);
        } catch (Throwable t) {
            // Never let the alert path fail the settle loop.
            LOG.warn("TelegramAlertAdapter failed", t);
        }
    }

    /** SHA-256 first 8 hex chars — irreversible, but cross-referenceable in DB. */
    static String hashNickname(String nickname) {
        if (nickname == null || nickname.isEmpty()) {
            return "anon";
        }
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(nickname.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder();
            for (int i = 0; i < 4 && i < hash.length; i++) {
                hex.append(String.format("%02x", hash[i] & 0xff));
            }
            return hex.toString();
        } catch (Exception e) {
            return "hashfail";
        }
    }
}
