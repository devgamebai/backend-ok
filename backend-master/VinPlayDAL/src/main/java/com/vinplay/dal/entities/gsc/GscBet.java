package com.vinplay.dal.entities.gsc;

import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * Plain entity for {@code vinplay.gsc_bets} rows — the MySQL mirror of
 * Mongo {@code log_gsc_bets}.
 *
 * <p>Field set is the union of the verified Mongo doc fields:
 * {@code _id, user_name, nick_name, bet_value, prize, fee, product_code,
 * game_code, game_key, game_name, txn_id, wager_code, currency, time_log,
 * create_time, bet_type, settled, settle_time, settle_txn_id} plus the
 * SUN-LIVE-HIST extras {@code vendor_game_id} (SUN-1196 freespin chain
 * link) and {@code event_key} (SUN-LIVE-HIST upsert match key).
 *
 * <p>The class is intentionally a plain field-bag — the DAO maps to/from
 * it via the prepared-statement column names. Keeping it free of
 * annotations / framework coupling matches the existing entity style in
 * {@code com.vinplay.dal.entities.*}.
 */
public class GscBet {

    /** Auto-increment id, set by MySQL. -1 before insert. */
    public long id = -1L;
    /** {@code users.id} for FK / fast user-scoped reads. */
    public long userId;
    /** Mongo equivalent: {@code user_name}. */
    public String userName;
    /** Mongo equivalent: {@code nick_name}. */
    public String nickName;
    /** Bet amount in sub-units. */
    public long betValue;
    /** Settled prize in sub-units. 0 until settle update. */
    public long prize;
    /** Tax / commission fee in sub-units. */
    public long fee;
    /** GSC product_code (e.g. 1052 = Dream Gaming). */
    public int productCode;
    public String gameCode;
    public String gameKey;
    public String gameName;
    public String txnId;
    /** UNIQUE — the natural key shared with Mongo. */
    public String wagerCode;
    public String currency;
    public String betType;
    public boolean settled;
    public Date settleTime;
    public String settleTxnId;
    /** SUN-1196 freespin chain link. */
    public String vendorGameId;
    /** SUN-LIVE-HIST de-dup / upsert match key. */
    public String eventKey;
    public Date createTime;
    public Date timeLog;

    /**
     * P6.1 — provider txn_ids merged into this row when {@code event_key}
     * upsert collapses sub-bets of a multi-bet hand. Mirrors Mongo's
     * {@code $addToSet txn_ids}. {@code null} = no merge yet (single-bet
     * row); on insert this is initialized from {@link #txnId}.
     */
    public List<String> txnIds;

    /**
     * P6.1 — provider settle txn_ids merged across multi-payout settles
     * for the same hand. Mirrors Mongo's {@code $addToSet settle_txn_ids}.
     */
    public List<String> settleTxnIds;

    /**
     * P6.1 — per-bet line breakdown for admin CMS drill-down + audit
     * exports. Mirrors Mongo's {@code $push details}. Schema:
     * {@code [{txn_id, amount, raw_amount, action}, ...]}.
     */
    public List<Map<String, Object>> details;

    public GscBet() {}
}
