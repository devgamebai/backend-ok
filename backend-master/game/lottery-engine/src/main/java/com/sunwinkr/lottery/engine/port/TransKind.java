package com.sunwinkr.lottery.engine.port;

/**
 * Wallet transaction marker. Mirrors {@code TransType.START_TRANS / IN_TRANS /
 * END_TRANS} in {@code com.vinplay.vbee.common.statics.TransType} without
 * dragging the legacy enum (or its 200-entry catalogue) into the pure-Java
 * engine module.
 *
 * <p>The adapter in PR-3 ({@code lottery-api}) maps this back to the legacy
 * enum at the JDBC boundary — see
 * {@code docs/plans/lottery-extraction-plan.md §2.5 S4}.
 *
 * <p>Lottery today uses {@link #START} for BOTH debit and credit (matches
 * legacy {@code LotteryModule.buyTicket} JLM:227 +
 * {@code LotteryModule.getResultLottery} JLM:129). The future
 * {@code TODO(SUN-LOTTERY-TRANSTYPE)} will split into LODE_WAGER / LODE_WIN
 * — until then, {@link #START} is the only used value.
 */
public enum TransKind {
    /** Wallet trans start — used by lottery debit + credit today. */
    START,
    /** Wallet trans intermediate — reserved, not used by lottery yet. */
    IN,
    /** Wallet trans end — reserved, not used by lottery yet. */
    END
}
