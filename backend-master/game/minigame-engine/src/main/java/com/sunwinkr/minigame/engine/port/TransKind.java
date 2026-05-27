package com.sunwinkr.minigame.engine.port;

/**
 * Engine-side mirror of the legacy {@code com.vinplay.vbee.common.enums
 * .TransType} enum. Adapter implementations map these to the integer
 * IDs persisted in {@code money_gateway_log}.
 *
 * <p>Mapping per rules-spec §6:
 * <ul>
 *   <li>{@link #START} → {@code TransType.START_TRANS} (1) — bet debit, opens logical txn</li>
 *   <li>{@link #IN}    → {@code TransType.IN_TRANS}    (2) — intermediate settle credit</li>
 *   <li>{@link #END}   → {@code TransType.END_TRANS}   (3) — final credit / refund close</li>
 * </ul>
 *
 * <p>The engine never references the legacy {@code TransType} class
 * directly — adapter ports translate at the boundary. This keeps the
 * engine module free of {@code com.vinplay.*} imports.
 *
 * <p>Sicbo PR-3 added two semantic-named entries ({@link #BET_DEBIT} and
 * {@link #REFUND_CREDIT}) that are kept as aliases so the engine bet
 * path is self-documenting. Adapter dispatch treats them identically to
 * {@link #START} and {@link #END} respectively.
 *
 * <p>Plan §2.2 row B5 / spec §6.
 */
public enum TransKind {
    /** Opens a logical txn — used for bet debit. */
    START,
    /** Intermediate credit during settlement (e.g. jackpot share). */
    IN,
    /** Final credit closing the txn (main prize, refund). */
    END,
    /** Sicbo PR-3 alias for {@link #START}. */
    BET_DEBIT,
    /** Sicbo PR-3 alias for {@link #END}. */
    REFUND_CREDIT
}
