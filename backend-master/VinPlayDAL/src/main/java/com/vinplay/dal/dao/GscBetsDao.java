package com.vinplay.dal.dao;

import com.vinplay.dal.entities.gsc.GscBet;

import java.sql.SQLException;
import java.util.Date;
import java.util.List;

/**
 * DAO for {@code vinplay.gsc_bets} — the MySQL mirror of Mongo
 * {@code log_gsc_bets}.
 *
 * <p>Cross-store traceability follow-up #5. Phase 5 prep gate 5p2
 * already routes the Mongo write through
 * {@code GscBetSideEffectProcessor}; this DAO is the parallel MySQL
 * write target during the dual-write soak. Reads stay on Mongo until a
 * separate post-soak cutover task flips them.
 *
 * <p>All methods are best-effort under the dual-write contract — the
 * caller in {@code GscBetSideEffectProcessor} catches every throw so a
 * MySQL failure can NOT cascade into a Mongo write or a wallet decision.
 *
 * <p>Mirrors the persistence operations the consumer + reconciler do
 * against Mongo today:
 * <ul>
 *   <li>{@link #insert} — BET_INSERT path. Two-mode behaviour mirroring
 *       the legacy publisher's {@code runBetInsert}:
 *       <ul>
 *         <li>{@code event_key == null} → INSERT IGNORE on
 *             {@code uk_wager_code} (single-bet row, retry-safe).</li>
 *         <li>{@code event_key != null} → INSERT ... ON DUPLICATE KEY
 *             UPDATE on {@code uk_event_key} (P6.2). Increments
 *             {@code bet_value} / {@code fee} and merges into
 *             {@code txn_ids} / {@code details} so multi-bet hands
 *             (c=303 baccarat, c=9843 dragon-tiger) collapse onto a
 *             single row matching Mongo's
 *             {@code $setOnInsert + $inc + $addToSet + $push} semantics.</li>
 *       </ul>
 *   </li>
 *   <li>{@link #settle} — SETTLE_UPDATE path. Two-tier filter (P6.3)
 *       mirroring {@code DepositProcess}'s Mongo {@code $or} candidate
 *       precedence: linkRoundId (freespin chain) → event_key →
 *       wager_code. Increments {@code prize}, flips {@code settled=1},
 *       and merges {@code settle_txn_id} into {@code settle_txn_ids}.</li>
 *   <li>{@link #deleteByWagerCode} — CANCEL_DELETE / ROLLBACK_DELETE
 *       path. DELETE WHERE wager_code = ?.</li>
 *   <li>{@link #deleteByFilter} — SUN-1184 free-spin row cleanup
 *       (drop bet_value=0 rows by user/product/game/wager filter).</li>
 *   <li>{@link #findUnsettledOlderThan} — Reconciler scan target.
 *       NOT used for read-flip yet; provided so the post-soak cutover
 *       has a working method to switch to.</li>
 *   <li>{@link #findByUserName} — c=303 / c=9843 history read target.
 *       NOT used for read-flip yet; same rationale.</li>
 * </ul>
 */
public interface GscBetsDao {

    /**
     * Insert (or upsert-merge when {@code bet.eventKey} is non-null).
     *
     * <p>{@code event_key == null} branch: plain INSERT IGNORE on
     * {@code uk_wager_code}. Returns true when a row was inserted, false
     * when the UNIQUE collided.
     *
     * <p>{@code event_key != null} branch: INSERT ... ON DUPLICATE KEY
     * UPDATE on {@code uk_event_key} — first call inserts, subsequent
     * calls for sub-bets of the same hand increment {@code bet_value}
     * and {@code fee}, append to {@code txn_ids} (de-duped via
     * {@code JSON_CONTAINS} guard), and push to {@code details}. Returns
     * true on insert, false on merge.
     *
     * <p>The caller populates {@link GscBet#txnIds} / {@link GscBet#details}
     * with single-element initial values for the insert path; the merge
     * path appends subsequent entries via SQL JSON functions.
     */
    boolean insert(GscBet bet) throws SQLException;

    /**
     * Apply a settle update with the two-tier filter from P6.3.
     *
     * <p>Filter precedence (mirrors legacy {@code DepositProcess}'s
     * Mongo {@code $or}):
     * <ol>
     *   <li>If {@code linkRoundId != null && !linkRoundId.isEmpty()}:
     *       freespin chain routing (SUN-1196). Filter:
     *       {@code user_name = ? AND product_code = ? AND vendor_game_id = ?}.</li>
     *   <li>Else if {@code eventKey != null && wagerCode != null}: try
     *       {@code event_key = ?} first; on rowcount=0 fall back to
     *       {@code (user_name, product_code, game_code, wager_code)}; on
     *       still-rowcount=0 fall back to {@code wager_code = ?}.</li>
     *   <li>Else if {@code wagerCode != null}: filter
     *       {@code wager_code = ?}.</li>
     *   <li>Else: filter {@code (user_name, game_code, settled = 0)}
     *       (legacy fallback).</li>
     * </ol>
     *
     * <p>Mirrors Mongo's {@code $inc prize / $set settled=true,
     * settle_time=now, settle_txn_id / $addToSet settle_txn_ids}.
     * Returns the number of rows updated (0 = no row matched any tier
     * — common during dual-write window when the BET_INSERT only ran on
     * Mongo).
     *
     * <p>Free-spin row cleanup (SUN-1184) is NOT performed here — the
     * caller decides whether to invoke {@link #deleteByFilter} after a
     * successful settle to drop phantom {@code bet_value=0} rows
     * matching the same filter.
     */
    int settle(String memberAccount,
               int productCode,
               String gameCode,
               String wagerCode,
               String eventKey,
               String linkRoundId,
               long prizeIncrement,
               String settleTxnId) throws SQLException;

    /**
     * DELETE the row(s) for {@code wager_code}. Mirrors Mongo
     * {@code col.deleteMany({wager_code})}. Returns delete count.
     */
    int deleteByWagerCode(String wagerCode) throws SQLException;

    /**
     * SUN-1184 free-spin cleanup — drop {@code bet_value=0} rows
     * matching the (user_name, product_code, game_code, wager_code)
     * filter that the Mongo settle path also drops. Returns delete count.
     *
     * <p>Any of the filter args may be null — null means "no constraint
     * on that column". {@code wagerCode} null with all other constraints
     * matches the Mongo settle path's free-spin filter when freespin
     * chain routing was used (vendor_game_id branch).
     */
    int deleteByFilter(String userName, Integer productCode,
                       String gameCode, String wagerCode) throws SQLException;

    /**
     * Reconciler scan target — rows with {@code settled=0} and
     * {@code create_time < cutoff}. Ordered by create_time ASC (oldest
     * first) and capped at {@code limit} so a single bad provider can't
     * monopolize the scan budget.
     *
     * <p>Mirrors {@link com.vinplay.dal.service.GscWagerReconciler}'s
     * Mongo find with {@code bet_value > 0} also enforced (matches
     * legacy filter exactly so post-cutover behaviour is identical).
     */
    List<GscBet> findUnsettledOlderThan(Date cutoff, int limit) throws SQLException;

    /**
     * History read target — rows for a given {@code user_name} ordered
     * by {@code create_time DESC} (newest first), with paging.
     *
     * <p>Mirrors Mongo's {@code find({user_name}).sort({create_time:-1})}
     * which is what {@link com.vinplay.dal.service.GameHistoryService}
     * c=303 / c=9843 endpoints read today.
     */
    List<GscBet> findByUserName(String userName, int limit, int offset) throws SQLException;
}
