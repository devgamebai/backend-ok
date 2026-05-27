package com.vinplay.dal.service.seamless.gsc;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.UpdateOptions;
import com.vinplay.dal.audit.TelegramOpsNotifier;
import com.vinplay.vbee.common.messagebus.MessageBusFactory;
import com.vinplay.vbee.common.messages.GscBetSideEffectMessage;
import com.vinplay.vbee.common.messages.GscBetSideEffectMessage.Op;
import com.vinplay.vbee.common.mongodb.MongoDBConnectionFactory;
import com.vinplay.vbee.common.mongodb.MongoRetry;
import org.apache.log4j.Logger;
import org.bson.Document;

import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Phase 5 prep gate 5p2 — publish GSC bet-side-effect messages to RMQ
 * so the Mongo {@code log_gsc_bets} write and the Telegram alert leave
 * the request hot path.
 *
 * <h2>Defense in depth — bus failure → run sync</h2>
 * If the publish fails (broker down, transient network), we fall back
 * to running the same Mongo + Telegram side effects synchronously in
 * the caller's thread. The cost is the original 5-10ms latency we
 * tried to avoid; the benefit is the {@code log_gsc_bets} row still
 * lands and ops still gets the alert. Without the fallback, a broker
 * outage would silently drop bet-history rows AND silence the
 * monitoring channel — exactly the failure mode 5p2 was meant to
 * prevent in the opposite direction.
 *
 * <p><b>Note (M-gsc):</b> the publish now goes through
 * {@link MessageBusFactory}. Per the {@link
 * com.vinplay.vbee.common.messagebus.MessageBus#publish MessageBus.publish}
 * contract, transport-level failures (RMQ broker dead, Redis down) are
 * logged-and-swallowed inside the bus adapter and do NOT throw out to
 * this caller. The {@code catch (Throwable)} below therefore only
 * fires on programmer error ({@link IllegalArgumentException} for
 * null args) — which never happens in practice because we null-check
 * {@code msg} above and {@code QUEUE_NAME} is a constant. The sync
 * fallback is preserved as belt-and-braces (and to keep the rollback
 * diff small) but is effectively unreachable on the legacy RMQ path.
 * Detecting bus-level outages now relies on the O1 audit-writer +
 * Grafana dashboards introduced in MR !323, not on this fallback.
 *
 * <p>The fallback path uses the same Mongo retry semantics as
 * {@link MongoRetry} so a transient Mongo blip during the fallback
 * still survives. The Telegram alert fires only if Mongo retries
 * exhaust (same contract as the consumer).
 *
 * <h2>Queue + binding</h2>
 * <ul>
 *   <li>Queue: {@code queue_log_gsc_bets_async}</li>
 *   <li>Command: {@code 1010}</li>
 *   <li>Bound to {@code GscBetSideEffectProcessor} in {@code api/vbee}'s
 *       {@code rabbitmq_config.xml}.</li>
 * </ul>
 */
public final class GscBetSideEffectPublisher {

    private static final Logger logger = Logger.getLogger("backend");

    public static final String QUEUE_NAME = "queue_log_gsc_bets_async";
    public static final int COMMAND_ID = 1010;

    private GscBetSideEffectPublisher() {}

    /**
     * Publish a side-effect message. On publish failure, falls back to
     * running the side effects synchronously so neither the Mongo row
     * nor the Telegram alert is silently lost.
     *
     * <p>M-gsc: the publish goes through {@link MessageBusFactory}.
     * {@link com.vinplay.vbee.common.messagebus.MessageBus#publish}
     * swallows transport errors internally per its contract, so the
     * fallback below only triggers on programmer-error
     * {@code IllegalArgumentException}s (which the null-check above
     * already prevents). The fallback is preserved as a structural
     * safety net and to keep the rollback diff minimal — see the class
     * javadoc for the full reasoning.
     */
    public static void publish(GscBetSideEffectMessage msg) {
        if (msg == null) return;
        try {
            MessageBusFactory.get(QUEUE_NAME).publish(QUEUE_NAME, msg, COMMAND_ID);
        } catch (Throwable rmqErr) {
            logger.warn("GscBetSideEffectPublisher: bus publish failed — running sync fallback."
                    + " op=" + msg.op
                    + " wager=" + msg.wagerCode
                    + " err=" + rmqErr.getMessage());
            try {
                runSync(msg);
            } catch (Throwable syncErr) {
                logger.warn("GscBetSideEffectPublisher: sync fallback also failed (non-fatal)."
                        + " op=" + msg.op
                        + " wager=" + msg.wagerCode
                        + " err=" + syncErr.getMessage());
                // The wallet credit/debit has already committed by the
                // time we reach this point — losing the log row is an
                // audit gap, not a money loss. The reconciler picks up
                // missed log_gsc_bets rows separately.
            }
        }
    }

    /**
     * Execute the side effects synchronously against a caller-supplied
     * Mongo collection. The consumer uses this entry point so the
     * publish+sync path and the consume+sync path share the exact same
     * business logic (and so unit tests can hand a mock collection).
     */
    public static void executeSideEffect(GscBetSideEffectMessage msg,
                                         MongoCollection<Document> col) throws Exception {
        if (msg == null || col == null || msg.op == null) return;
        switch (msg.op) {
            case BET_INSERT:
                runBetInsert(msg, col);
                break;
            case SETTLE_UPDATE:
                runSettleUpdate(msg, col);
                break;
            case CANCEL_DELETE:
            case ROLLBACK_DELETE:
                runDelete(msg, col);
                break;
            case FREESPIN_CHAIN_SETTLE:
                // Reserved for future evolution; same as SETTLE_UPDATE today.
                runSettleUpdate(msg, col);
                break;
        }
    }

    // ─────────────────────────────────────────────────────────────────
    // Sync fallback (publisher path, RMQ unavailable)
    // ─────────────────────────────────────────────────────────────────

    private static void runSync(GscBetSideEffectMessage msg) throws Exception {
        MongoCollection<Document> col = MongoDBConnectionFactory.getDB()
                .getCollection("log_gsc_bets");
        try {
            MongoRetry.runWithRetry(
                    msg.aggregatorTag + " sync " + msg.op + " wager=" + msg.wagerCode,
                    () -> executeSideEffect(msg, col));
        } catch (Throwable mongoErr) {
            logger.warn("GscBetSideEffectPublisher.runSync: Mongo failed (non-fatal)."
                    + " op=" + msg.op
                    + " wager=" + msg.wagerCode
                    + " member=" + msg.memberAccount
                    + " err=" + mongoErr.getMessage());
            fireTelegramIfRequested(msg, mongoErr.getMessage());
            throw mongoErr instanceof Exception ? (Exception) mongoErr : new RuntimeException(mongoErr);
        }
    }

    /**
     * Fire the Telegram alert when the message asked for one and the
     * Mongo retry exhausted. Keeping this in the publisher (vs. inline
     * at every call site) ensures the alert wording is identical
     * across the sync-fallback and the async-consumer paths.
     */
    public static void fireTelegramIfRequested(GscBetSideEffectMessage msg, String errMessage) {
        if (msg == null || msg.telegramAlertSubject == null) return;
        try {
            TelegramOpsNotifier.alertGscBetWriteFailure(
                    msg.telegramAlertSubject,
                    msg.wagerCode,
                    msg.memberAccount,
                    msg.amount,
                    errMessage);
        } catch (Throwable alertErr) {
            // TelegramOpsNotifier already swallows internally; defense.
        }
    }

    // ─────────────────────────────────────────────────────────────────
    // Mongo-op implementations (mirror the legacy aggregator code byte-for-byte)
    // ─────────────────────────────────────────────────────────────────

    private static void runBetInsert(GscBetSideEffectMessage msg,
                                     MongoCollection<Document> col) {
        long nowMs = msg.createdAtMs > 0L ? msg.createdAtMs : System.currentTimeMillis();
        String timeStr = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date(nowMs));

        // SUN-1246: race-free upsert. The legacy `insertOne` lost data
        // when SETTLE_UPDATE for the same wager_code arrived FIRST
        // (vbee runs 4 parallel consumer threads, RMQ doesn't preserve
        // per-key order). With commutative upserts, BET_INSERT and
        // SETTLE_UPDATE write disjoint field sets — whichever lands
        // first creates the row + reserves the other op's fields via
        // $setOnInsert; whichever lands second fills in its own fields
        // without overwriting. Result is order-independent.
        //
        // Field ownership:
        //   BET_INSERT  ($set):         bet-side fields
        //   SETTLE_UPDATE ($set/$inc):  settled, settle_time, settle_txn_id, prize
        //   Either one  ($setOnInsert): "the other op's fields" — defaults
        //                               that the second op overwrites if
        //                               they're its turf, or remain untouched
        //                               if it never arrives.
        Document betSet = new Document()
                .append(com.vinplay.dal.service.seamless.SeamlessProvider.FIELD,
                        com.vinplay.dal.service.seamless.SeamlessProvider.GSC)
                .append("user_name", msg.memberAccount)
                .append("nick_name", msg.memberAccount)
                .append("product_code", msg.productCode)
                .append("game_code", msg.gameCode)
                .append("game_key", msg.gameKey)
                .append("game_name", msg.gameName != null ? msg.gameName : msg.gameKey)
                .append("txn_id", msg.txnId)
                .append("wager_code", msg.wagerCode)
                .append("currency", msg.currency)
                .append("time_log", timeStr)
                .append("create_time", new Date(nowMs))
                .append("bet_type", "BET");
        if (msg.vendorGameId != null && !msg.vendorGameId.isEmpty()) {
            betSet.append("vendor_game_id", msg.vendorGameId);
        }
        // SUN-1248 / Phase 2: stamp pre-debit balance on the BET row so
        // the agency LS Cược (c=9843) reader returns money_before > 0
        // without needing the log_money_user_vin walk-back. >0 guard
        // skips legacy producers that haven't been redeployed yet.
        if (msg.currentMoneyBefore > 0L) {
            betSet.append("current_money", msg.currentMoneyBefore);
        }

        // Defaults the SETTLE side will overwrite later. Only fires when
        // we're the first arrival.
        Document setOnInsertDefaults = new Document()
                .append("settled", false)
                .append("prize", 0L);

        // Filter: prefer event_key when present (mirrors legacy multi-tx
        // bet aggregation), fall back to wager_code so SETTLE_UPDATE can
        // find the same row even when the BET message had no event_key.
        Document filter = (msg.eventKey != null && !msg.eventKey.isEmpty())
                ? new Document("event_key", msg.eventKey)
                : new Document("wager_code", msg.wagerCode);

        if (msg.eventKey == null || msg.eventKey.isEmpty()) {
            // Single-tx bet: bet_value + fee in $set (no $inc needed).
            betSet.append("bet_value", msg.amount).append("fee", msg.fee);
            Document update = new Document("$set", betSet)
                    .append("$setOnInsert", setOnInsertDefaults);
            col.updateOne(filter, update, new UpdateOptions().upsert(true));
            return;
        }

        // event_key path: multiple sub-tx may target one logical row,
        // accumulate bet_value + fee via $inc (legacy parity).
        betSet.append("event_key", msg.eventKey);
        Document detail = new Document()
                .append("txn_id", msg.txnId)
                .append("amount", msg.amount)
                .append("raw_amount", msg.rawAmount)
                .append("action", msg.action);

        Document update = new Document("$set", betSet)
                .append("$setOnInsert", setOnInsertDefaults)
                .append("$inc", new Document("bet_value", msg.amount).append("fee", msg.fee))
                .append("$addToSet", new Document("txn_ids", msg.txnId))
                .append("$push", new Document("details", detail));
        col.updateOne(filter, update, new UpdateOptions().upsert(true));
    }

    private static void runSettleUpdate(GscBetSideEffectMessage msg,
                                        MongoCollection<Document> col) {
        // SUN-1246: race-free upsert. See runBetInsert for the design
        // rationale — when SETTLE arrives BEFORE BET (parallel-consumer
        // race), upsert creates the row with settle data + $setOnInsert
        // placeholders for the bet-side fields; BET_INSERT later
        // overwrites the placeholders via $set without disturbing
        // the settle data.
        //
        // The freespin-chain filter (linkRoundId) and $or candidate
        // filter cannot use upsert safely (ambiguous row creation), so
        // those paths fall back to plain updateOne — which is still
        // correct because they run AFTER BET_INSERT for those flows.
        // The wager_code path is the common case and is the one that
        // races with BET_INSERT.
        boolean isFreespinChain = msg.linkRoundId != null && !msg.linkRoundId.isEmpty();
        boolean useUpsert = !isFreespinChain
                && msg.wagerCode != null && !msg.wagerCode.isEmpty();
        Document filter;
        if (isFreespinChain) {
            filter = new Document("user_name", msg.memberAccount)
                    .append("product_code", msg.productCode)
                    .append("vendor_game_id", msg.linkRoundId);
        } else if (msg.eventKey != null && msg.wagerCode != null && !msg.wagerCode.isEmpty()) {
            // event_key + wager_code: prefer event_key for upsert (avoids
            // creating a duplicate row when BET landed first under
            // event_key but our $or would also match wager_code-only
            // rows). The single-key filter is upsert-safe.
            filter = new Document("event_key", msg.eventKey);
        } else if (msg.wagerCode != null && !msg.wagerCode.isEmpty()) {
            filter = new Document("wager_code", msg.wagerCode);
        } else {
            filter = new Document("user_name", msg.memberAccount)
                    .append("game_code", msg.gameCode)
                    .append("settled", false);
            useUpsert = false;
        }

        Document setFields = new Document()
                .append("settled", true)
                .append("settle_time", new Date())
                .append("settle_txn_id", msg.txnId);
        if (msg.eventKey != null) {
            setFields.append("event_key", msg.eventKey);
        }
        // SUN-1248 / Phase 2: stamp post-settle balance so the agency
        // reader gets money_after directly. >0 guard keeps backwards
        // compat with legacy producers.
        if (msg.currentMoneyAfter > 0L) {
            setFields.append("current_money_after", msg.currentMoneyAfter);
        }

        // SUN-1370 — stamp the provider's valid_bet_amount on settle so the
        // agency rolling history shows commission-eligible bet, not the raw
        // total (Dream Gaming pushes/voids reduce total but not valid_bet,
        // making the displayed bet not reconcile against prize).
        if (msg.validBetAmount > 0L) {
            setFields.append("valid_bet_value", msg.validBetAmount);
        }
        // SUN-1367 — stamp the lossless milli sister of valid_bet so the
        // reader can show 2-decimal precision matching the vendor iframe.
        // Reuses `bet_value_milli` so the backfill from `gsc_event_log`
        // and live writes share one field name. 0 = legacy/unset; reader
        // falls back to `bet_value × 1000`.
        if (msg.validBetAmountMilli > 0L) {
            setFields.append("bet_value_milli", msg.validBetAmountMilli);
        }

        // $setOnInsert defaults — only fire when the upsert creates the
        // row (i.e. SETTLE arrived before BET). These are placeholder
        // values; BET_INSERT will overwrite them via $set. Leaving the
        // bet-side fields semantically marked so a BET_INSERT that
        // never arrives still leaves a queryable row visible to ops.
        Document setOnInsert = new Document()
                .append("user_name", msg.memberAccount)
                .append("nick_name", msg.memberAccount)
                .append("product_code", msg.productCode)
                .append("game_code", msg.gameCode != null ? msg.gameCode : "")
                .append("wager_code", msg.wagerCode)
                .append("create_time", new Date())
                .append("bet_type", "BET")
                .append("bet_value", 0L)
                .append("fee", 0L)
                .append("currency", msg.currency != null ? msg.currency : "")
                .append("settle_arrived_first", true);

        // SUN-1367 — accumulate the lossless milli prize alongside the
        // legacy integer prize. Both stay in sync; reader prefers milli
        // when present.
        Document inc = new Document("prize", msg.prize);
        if (msg.prizeMilli > 0L) {
            inc.append("prize_milli", msg.prizeMilli);
        }
        Document update = new Document("$inc", inc)
                .append("$set", setFields)
                .append("$addToSet", new Document("settle_txn_ids", msg.txnId));
        if (useUpsert) {
            update.append("$setOnInsert", setOnInsert);
            com.mongodb.client.result.UpdateResult ur =
                    col.updateOne(filter, update, new UpdateOptions().upsert(true));
            if (ur.getUpsertedId() != null) {
                logger.info("[SUN-1246] settle arrived before bet — upsert created stub for wager="
                        + msg.wagerCode + " member=" + msg.memberAccount
                        + ". BET_INSERT will fill bet-side fields when it lands.");
            }
        } else {
            com.mongodb.client.result.UpdateResult ur = col.updateOne(filter, update);
            if (ur.getMatchedCount() == 0) {
                // No upsert-safe filter (freespin-chain / member+game)
                // and no row matched — log loudly so ops can reconcile
                // from gsc_event_log. Previously this was silent.
                logger.warn("[SUN-1246] runSettleUpdate matched 0 rows (no upsert-safe filter):"
                        + " wager=" + msg.wagerCode
                        + " member=" + msg.memberAccount
                        + " filter=" + filter.toJson());
            }
        }

        // SUN-1184 free-spin row cleanup — drop any phantom row matching
        // the same filter with bet_value==0. Best-effort; legacy logs
        // and continues on failure. We exclude rows that just got
        // upserted by SUN-1246's path (settle_arrived_first=true) so
        // we don't accidentally delete the stub before BET fills it.
        try {
            Document phantomFilter = new Document(filter)
                    .append("bet_value", 0L)
                    .append("settle_arrived_first", new Document("$ne", true));
            col.deleteMany(phantomFilter);
        } catch (Throwable cleanupErr) {
            logger.warn("[SUN-1184] free-spin row cleanup failed (non-fatal): "
                    + cleanupErr.getMessage()
                    + " wager=" + msg.wagerCode
                    + " member=" + msg.memberAccount);
        }
    }

    private static void runDelete(GscBetSideEffectMessage msg,
                                  MongoCollection<Document> col) {
        if (msg.wagerCode == null || msg.wagerCode.trim().isEmpty()) return;
        long deleted = col.deleteMany(new Document("wager_code", msg.wagerCode.trim()))
                .getDeletedCount();
        logger.info("[SUN-1182] " + msg.aggregatorTag + " " + msg.op + " log_gsc_bets cleanup wager="
                + msg.wagerCode + " deleted=" + deleted);
    }
}
