package com.vinplay.vbee.common.response;

import org.apache.log4j.Logger;

/**
 * Safety guard for "current balance" fields on response DTOs sent to the client.
 *
 * <p>History: SUN-748 / SUN-753 / SUN-764 follow-ups (2026-04-10) — multiple
 * bugs where processors accidentally sent the cumulative P&amp;L counter
 * ({@code vin_total}/{@code xu_total}) as the "current balance" field on a
 * response. For any losing player ({@code vin_total &lt; 0}) this caused the
 * wallet to display as a large negative number immediately after the next
 * action, even though the real balance was positive.
 *
 * <p>The root cause was always the same: {@code UserModel.getCurrentMoney(type)}
 * is named like "current balance" but returns {@code vin_total}/{@code xu_total}
 * (cumulative game P&amp;L). Every developer who read the name assumed it was
 * the current balance, and the bug kept reappearing in different places:
 * <ul>
 *   <li>{@code UserServiceImpl.updateMoney} — fixed by SUN-748
 *   <li>{@code UserServiceImpl.napXu} — fixed by SUN-753 follow-up
 *   <li>{@code UserServiceImpl.transferMoney} (multiple branches) — latent
 * </ul>
 *
 * <p>This guard is the last line of defence: it clamps any negative "balance"
 * value to zero before it ships to the client, and logs a loud stack trace so
 * the leaking processor can be identified and fixed at the source. It is
 * deliberately verbose: swallowing the value silently would hide real bugs
 * forever; the stack trace pin-points the caller.
 *
 * <p><b>Scope</b>: apply this guard ONLY to response DTOs that ship CURRENT
 * BALANCE to the game client (e.g. {@link MoneyResponse#setCurrentMoney(long)},
 * {@link NapXuResponse#setCurrentMoneyVin(long)}).
 * <p>Do NOT apply to log or stat messages ({@code LogMoneyUserMessage},
 * {@code MoneyMessageInMinigame}): those legitimately carry cumulative P&amp;L
 * counters that can be negative.
 */
public final class BalanceGuard {

    private static final Logger logger = Logger.getLogger("BalanceGuard");

    private BalanceGuard() {}

    /**
     * Clamp a balance-type wire value to {@code [0, +infinity)}. Logs a loud
     * stack trace if the input is negative so the caller can be traced and
     * fixed.
     *
     * @param value     the value the caller is trying to ship to the client
     * @param fieldName human-readable identifier for the response field, used
     *                  in the log message (e.g. {@code "MoneyResponse.currentMoney"})
     * @return {@code value} if non-negative; {@code 0} if negative
     */
    public static long clamp(long value, String fieldName) {
        if (value >= 0) return value;
        logger.error("NEGATIVE BALANCE LEAK: " + fieldName + "=" + value
                + " (clamped to 0). Caller passed vin_total/xu_total where "
                + "vin/xu was intended. See SUN-748/SUN-753 class of bugs.",
                new IllegalStateException("negative balance setter"));
        return 0;
    }
}
