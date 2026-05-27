package com.vinplay.vbee.common.messages;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Phase 5 prep gate 5p2 — async side-effect envelope for GSC aggregators.
 *
 * <p>The GSC seamless aggregators ({@link
 * com.vinplay.vbee.common.messages.LogMoneyUserMessage} pipeline aside)
 * historically did Mongo {@code log_gsc_bets} writes and Telegram
 * {@code TelegramOpsNotifier} alerts <em>synchronously</em> inside the
 * {@code afterCredit}/{@code afterDebit} hot path. Under burst load that
 * adds 5-10ms to handler latency and — worse — chains a Mongo backoff
 * onto the MySQL pool because the request thread holds the
 * {@code mysqlpoolname} connection through the Mongo retry window.
 *
 * <p>5p2 moves both side-effects to a dedicated RMQ queue
 * ({@code queue_log_gsc_bets_async}) drained by a thread pool in
 * {@code api/vbee} (see {@code GscBetSideEffectProcessor}). The wallet
 * credit/debit stays synchronous (correctness invariant); only the
 * post-wallet bookkeeping moves async.
 *
 * <h2>Safety analysis</h2>
 * <ol>
 *   <li>{@code gsc_event_log} writes stay synchronous (in the base
 *       class's {@code preAudit}/{@code postAudit}, not in the per-
 *       aggregator hooks). The reconciler's RECEIVED-state guard reads
 *       {@code gsc_event_log}, so it is not affected by async-write
 *       delay on {@code log_gsc_bets}.</li>
 *   <li>The reconciler also keys on a {@code create_time < NOW() -
 *       graceWindowMs} grace window (production default 5 minutes).
 *       Async-consumer typical landing latency is well under 1 second,
 *       absorbed by the grace window — no double-credit risk.</li>
 * </ol>
 *
 * <h2>Field-access convention</h2>
 * Fields are {@code public} for direct access — this is an internal
 * message bus with no API stability concern. Mirrors the pattern used
 * by {@link com.vinplay.vbee.common.response.LogMoneyUserResponse}.
 *
 * <h2>Op enum</h2>
 * The {@link Op} discriminator tells the consumer which Mongo operation
 * to run. Each {@code afterCredit}/{@code afterDebit} site picks exactly
 * one op based on the legacy parity branch:
 * <ul>
 *   <li>{@link Op#BET_INSERT} — Withdraw normal bet path
 *       ({@code GscWithdrawAggregator.afterDebit}). Insert or upsert
 *       merge into {@code log_gsc_bets} keyed on {@code event_key}.</li>
 *   <li>{@link Op#SETTLE_UPDATE} — Deposit normal settle path
 *       ({@code GscDepositAggregator.afterCredit}). $inc prize / $set
 *       settled / $addToSet settle_txn_ids. SUN-1196 freespin chain
 *       routing handled by setting {@link #linkRoundId}.</li>
 *   <li>{@link Op#CANCEL_DELETE} — Deposit cancel-via-deposit path
 *       (SUN-1182). Drop {@code log_gsc_bets} rows by wager_code.</li>
 *   <li>{@link Op#ROLLBACK_DELETE} — Rollback path. Drop
 *       {@code log_gsc_bets} rows by wager_code.</li>
 *   <li>{@link Op#FREESPIN_CHAIN_SETTLE} — Reserved for future
 *       freespin-chain handling that needs distinct semantics from
 *       {@link Op#SETTLE_UPDATE}. Currently unused; carrying it lets
 *       the consumer evolve without a wire-format change.</li>
 * </ul>
 *
 * <h2>Telegram alert</h2>
 * When non-null, {@link #telegramAlertSubject} carries the {@code op}
 * label that {@link com.vinplay.vbee.common.messages.GscBetSideEffectMessage}'s
 * consumer fires through {@code TelegramOpsNotifier.alertGscBetWriteFailure}
 * if and only if the consumer's Mongo retry exhausts. Producers do NOT
 * fire Telegram themselves — that decision moves entirely to the
 * consumer so a defense-in-depth sync fallback in the publisher does
 * not double-alert.
 */
public class GscBetSideEffectMessage extends BaseMessage {

    private static final long serialVersionUID = 1L;

    /** Discriminator — see class doc. */
    public enum Op {
        BET_INSERT,
        SETTLE_UPDATE,
        ROLLBACK_DELETE,
        CANCEL_DELETE,
        FREESPIN_CHAIN_SETTLE
    }

    public Op op;

    public String wagerCode;
    public String memberAccount;
    /**
     * P6.4 — resolved {@code users.id} carried through from the
     * aggregator. The aggregator's wallet path already loaded the user
     * to do the credit/debit, so we pass the id rather than making the
     * consumer re-resolve from Hazelcast/MySQL (where both can miss and
     * write {@code user_id=0} into the audit row).
     *
     * <p>Producers SHOULD set this. {@code 0} = "unset" — the consumer
     * falls back to its Hazelcast/MySQL lookup for backward compat with
     * any caller that hasn't been updated yet, but logs a WARN.
     */
    public long userId;
    public int productCode;
    public String gameCode;

    /** Bet amount (sub-units) for {@link Op#BET_INSERT}. */
    public long amount;

    /** Settle prize amount (sub-units) for {@link Op#SETTLE_UPDATE}. */
    public long prize;

    /** Hedge-bet commission volume override (SUN-1205/1206). 0 = none. */
    public long validBetAmount;

    /**
     * SUN-1367 — decimal-precision sister fields. Provider wires that
     * ship fractional amounts (e.g. Dream Gaming IDR "63096.15") would
     * otherwise truncate to integer subunit at the {@code amount} /
     * {@code prize} fields. The aggregator parses the raw payload string
     * with {@code BigDecimal} at the input boundary and writes the
     * lossless ×1000 value here. {@code 0} = unset (legacy producer
     * before SUN-1367, or provider whose wire is already integer).
     *
     * <p>Consumer ({@code GscBetSideEffectPublisher}) stamps these as
     * {@code bet_value_milli} / {@code prize_milli} / {@code valid_bet_milli}
     * on {@code log_gsc_bets}. {@code GameHistoryService.fetchOneMongoSource}
     * prefers the milli value when present and renders at 2-decimal
     * precision matching the vendor iframe.
     */
    public long amountMilli;
    public long prizeMilli;
    public long validBetAmountMilli;

    /** Provider transaction id (used to populate $addToSet txn_ids / settle_txn_ids). */
    public String txnId;

    /** Raw provider amount string for the per-bet detail array. */
    public String rawAmount;

    /** Action verb for the per-bet detail array. */
    public String action;

    /** Computed event_key from {@code buildGscBetEventKey} (may be null). */
    public String eventKey;

    /** Resolved rebate game key (e.g. "gsc_1002_baccA"); used as {@code game_key}. */
    public String gameKey;

    /** Display name resolved via {@code GscGameNameResolver}. */
    public String gameName;

    /** Provider currency for the audit row. */
    public String currency;

    /** SUN-1196 freespin chain id (parent_round_id). null = no chain. */
    public String vendorGameId;

    /** SUN-1196 deposit-side: when set, settle routes by (user, product, vendor_game_id). */
    public String linkRoundId;

    /** Tax fee (sub-units) for the bet row. */
    public long fee;

    /** Bet creation timestamp (epoch millis). The consumer formats as the row's create_time. */
    public long createdAtMs;

    /**
     * SUN-1248 / Phase 2 — pre-debit player balance, stamped on BET_INSERT
     * so {@code log_gsc_bets.current_money} carries the player's vin BEFORE
     * the wager was posted. Powers near-real-time {@code money_before} in
     * the agency LS Cược / c=9843 view; without this the reader fell back
     * to {@code current_money=0} and the UI showed 0. {@code 0} = unset
     * (legacy producer; consumer should leave the field absent on the
     * Mongo doc so the supplement walk-back can still try).
     */
    public long currentMoneyBefore;

    /**
     * SUN-1248 / Phase 2 — post-settle player balance, stamped on
     * SETTLE_UPDATE. Powers {@code money_after} in the agency view.
     * {@code 0} = unset.
     */
    public long currentMoneyAfter;

    /** Provider-specific extras (payload dump, agent meta, etc.) — currently unused. */
    public Map<String, Object> extra;

    /**
     * Pre-formatted Telegram subject (the {@code op} label fed into
     * {@code TelegramOpsNotifier.alertGscBetWriteFailure}'s {@code op}
     * argument). {@code null} = consumer should NOT fire an alert
     * regardless of failure outcome. Non-null = fire alert if the
     * consumer's Mongo retries exhaust.
     */
    public String telegramAlertSubject;

    /** Per-handler tag for telemetry / debug logs (e.g. {@code "GscWithdraw"}). */
    public String aggregatorTag;

    public GscBetSideEffectMessage() {
        // No-arg constructor required for Java serialization round-trip
        // through the RMQ broker (BaseMessage uses ObjectInputStream).
    }

    /**
     * Convenience builder for the small subset of producers that prefer
     * the immutable-field style. Mutators stay public so test helpers
     * and the consumer's deserialization path keep working unchanged.
     */
    public static GscBetSideEffectMessage of(Op op,
                                             String aggregatorTag,
                                             String memberAccount,
                                             int productCode,
                                             String gameCode,
                                             String wagerCode,
                                             String txnId,
                                             long createdAtMs) {
        GscBetSideEffectMessage m = new GscBetSideEffectMessage();
        m.op = op;
        m.aggregatorTag = aggregatorTag;
        m.memberAccount = memberAccount;
        m.productCode = productCode;
        m.gameCode = gameCode;
        m.wagerCode = wagerCode;
        m.txnId = txnId;
        m.createdAtMs = createdAtMs;
        m.extra = new LinkedHashMap<>();
        return m;
    }
}
