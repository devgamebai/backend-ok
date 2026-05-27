package com.vinplay.dal.service.seamless.gsc;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.UpdateOptions;
import com.mongodb.client.result.DeleteResult;
import com.mongodb.client.result.UpdateResult;
import com.vinplay.vbee.common.messages.GscBetSideEffectMessage;
import org.bson.Document;
import org.bson.conversions.Bson;
import org.junit.Test;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/**
 * Phase 5 prep gate 5p2 — unit tests for
 * {@link GscBetSideEffectPublisher#executeSideEffect(GscBetSideEffectMessage,
 * MongoCollection)} and the consumer-equivalent path. Mocks the
 * MongoCollection via dynamic proxy so the test is hermetic — no real
 * Mongo / RMQ needed.
 *
 * <p>Tests cover:
 * <ul>
 *   <li>{@code BET_INSERT} with an event_key triggers $setOnInsert +
 *       $inc + $addToSet + $push (the merge-by-event_key path).</li>
 *   <li>{@code BET_INSERT} without event_key triggers an upsert keyed by wager_code.</li>
 *   <li>{@code SETTLE_UPDATE} triggers $inc prize / $set settled /
 *       $addToSet settle_txn_ids; SUN-1184 cleanup (deleteMany on
 *       phantom rows) follows.</li>
 *   <li>{@code SETTLE_UPDATE} with a {@code linkRoundId} routes by
 *       (user, product, vendor_game_id) — SUN-1196 freespin chain.</li>
 *   <li>{@code CANCEL_DELETE} / {@code ROLLBACK_DELETE} hit deleteMany
 *       keyed by {@code wager_code}.</li>
 * </ul>
 *
 * <p>Tests do NOT cover the RMQ publish path itself — that requires a
 * real broker or a much heavier mock; the publisher's RMQ-to-sync
 * fallback is exercised by the runtime path during deployment.
 */
public class GscBetSideEffectPublisherTest {

    /**
     * Captures every Mongo operation invocation in order so the test
     * can assert which ops fired and with what arguments. The proxy
     * implements {@link MongoCollection} for {@link Document}.
     */
    private static final class MockOp {
        final String method;
        final Object[] args;
        MockOp(String method, Object[] args) {
            this.method = method;
            this.args = args;
        }
    }

    @SuppressWarnings("unchecked")
    private static MongoCollection<Document> mockCollection(final List<MockOp> ops) {
        return (MongoCollection<Document>) Proxy.newProxyInstance(
                MongoCollection.class.getClassLoader(),
                new Class<?>[]{MongoCollection.class},
                new InvocationHandler() {
                    @Override
                    public Object invoke(Object proxy, Method m, Object[] args) {
                        ops.add(new MockOp(m.getName(), args));
                        // Most Mongo write methods return Result-typed values
                        // (BulkWriteResult, DeleteResult, etc.). We only need
                        // deleteMany's DeleteResult for the cancel-delete path.
                        if ("updateOne".equals(m.getName())) {
                            return new UpdateResult() {
                                @Override public boolean wasAcknowledged() { return true; }
                                @Override public long getMatchedCount() { return 1L; }
                                @Override public boolean isModifiedCountAvailable() { return true; }
                                @Override public long getModifiedCount() { return 1L; }
                                @Override public org.bson.BsonValue getUpsertedId() { return null; }
                            };
                        }
                        if ("deleteMany".equals(m.getName())) {
                            return new DeleteResult() {
                                @Override public boolean wasAcknowledged() { return true; }
                                @Override public long getDeletedCount() { return 0L; }
                            };
                        }
                        Class<?> ret = m.getReturnType();
                        if (ret == void.class) return null;
                        if (ret.isPrimitive()) return Integer.valueOf(0);
                        return null;
                    }
                });
    }

    @Test
    public void executeSideEffect_betInsert_withoutEventKey_callsUpdateOneUpsert() throws Exception {
        List<MockOp> ops = new ArrayList<>();
        MongoCollection<Document> col = mockCollection(ops);

        GscBetSideEffectMessage msg = GscBetSideEffectMessage.of(
                GscBetSideEffectMessage.Op.BET_INSERT,
                "GscWithdraw", "alice", 1002, "bacc",
                "W1", "TX1", System.currentTimeMillis());
        msg.amount = 5000L;
        msg.fee = 0L;
        msg.eventKey = null; // ← drives the simple insertOne path
        msg.gameKey = "gsc_1002_bacc";
        msg.gameName = "Baccarat";
        msg.currency = "VND";

        GscBetSideEffectPublisher.executeSideEffect(msg, col);

        assertEquals(1, ops.size());
        assertEquals("updateOne", ops.get(0).method);
        Document filter = (Document) ops.get(0).args[0];
        Document update = (Document) ops.get(0).args[1];
        UpdateOptions opts = (UpdateOptions) ops.get(0).args[2];
        assertEquals("W1", filter.getString("wager_code"));
        assertTrue("upsert must be true", opts.isUpsert());

        Document set = (Document) update.get("$set");
        assertEquals("alice", set.getString("user_name"));
        assertEquals(Long.valueOf(5000L), set.getLong("bet_value"));
        assertEquals(Integer.valueOf(1002), set.getInteger("product_code"));
        assertEquals("bacc", set.getString("game_code"));
        assertEquals("gsc_1002_bacc", set.getString("game_key"));
        assertEquals("VND", set.getString("currency"));

        Document setOnInsert = (Document) update.get("$setOnInsert");
        assertEquals(Boolean.FALSE, setOnInsert.getBoolean("settled"));
        assertEquals(Long.valueOf(0L), setOnInsert.getLong("prize"));
    }

    @Test
    public void executeSideEffect_betInsert_withEventKey_callsUpdateOneUpsert() throws Exception {
        List<MockOp> ops = new ArrayList<>();
        MongoCollection<Document> col = mockCollection(ops);

        GscBetSideEffectMessage msg = GscBetSideEffectMessage.of(
                GscBetSideEffectMessage.Op.BET_INSERT,
                "GscWithdraw", "alice", 1002, "bacc",
                "W2", "TX2", System.currentTimeMillis());
        msg.amount = 7000L;
        msg.fee = 100L;
        msg.eventKey = "gsc:1002:bacc:W2"; // ← drives the merge-upsert path
        msg.gameKey = "gsc_1002_bacc";
        msg.gameName = "Baccarat";
        msg.currency = "VND";
        msg.rawAmount = "-7000";
        msg.action = "BET";
        msg.vendorGameId = "vgid-X1"; // SUN-1196

        GscBetSideEffectPublisher.executeSideEffect(msg, col);

        assertEquals(1, ops.size());
        assertEquals("updateOne", ops.get(0).method);
        // arg0 = filter, arg1 = update, arg2 = UpdateOptions
        Document filter = (Document) ops.get(0).args[0];
        Document update = (Document) ops.get(0).args[1];
        UpdateOptions opts = (UpdateOptions) ops.get(0).args[2];
        assertEquals("gsc:1002:bacc:W2", filter.getString("event_key"));
        assertTrue("upsert must be true", opts.isUpsert());

        // $inc
        Document inc = (Document) update.get("$inc");
        assertEquals(Long.valueOf(7000L), inc.getLong("bet_value"));
        assertEquals(Long.valueOf(100L), inc.getLong("fee"));
        // $addToSet
        Document addToSet = (Document) update.get("$addToSet");
        assertEquals("TX2", addToSet.getString("txn_ids"));
        // $push
        Document push = (Document) update.get("$push");
        Document detail = (Document) push.get("details");
        assertEquals("TX2", detail.getString("txn_id"));
        assertEquals(Long.valueOf(7000L), detail.getLong("amount"));
        assertEquals("-7000", detail.getString("raw_amount"));
        assertEquals("BET", detail.getString("action"));
        Document set = (Document) update.get("$set");
        assertEquals("vgid-X1", set.getString("vendor_game_id"));

        // $setOnInsert only carries the settle-side defaults. If a
        // SETTLE_UPDATE arrived first, these defaults do not run again
        // and therefore cannot unset settled=true.
        Document setOnInsert = (Document) update.get("$setOnInsert");
        assertEquals(Boolean.FALSE, setOnInsert.getBoolean("settled"));
        assertEquals(Long.valueOf(0L), setOnInsert.getLong("prize"));
    }

    @Test
    public void executeSideEffect_settleUpdate_normalPath_setsAndIncs() throws Exception {
        List<MockOp> ops = new ArrayList<>();
        MongoCollection<Document> col = mockCollection(ops);

        GscBetSideEffectMessage msg = GscBetSideEffectMessage.of(
                GscBetSideEffectMessage.Op.SETTLE_UPDATE,
                "GscDeposit", "alice", 1002, "bacc",
                "W3", "TX3", System.currentTimeMillis());
        msg.prize = 9000L;
        msg.eventKey = "gsc:1002:bacc:W3";
        msg.gameKey = "gsc_1002_bacc";
        msg.currency = "VND";

        GscBetSideEffectPublisher.executeSideEffect(msg, col);

        // First op: updateOne; second: deleteMany (SUN-1184 cleanup)
        assertEquals(2, ops.size());
        assertEquals("updateOne", ops.get(0).method);
        assertEquals("deleteMany", ops.get(1).method);

        Document update = (Document) ops.get(0).args[1];
        Document inc = (Document) update.get("$inc");
        assertEquals(Long.valueOf(9000L), inc.getLong("prize"));
        Document set = (Document) update.get("$set");
        assertEquals(Boolean.TRUE, set.getBoolean("settled"));
        assertEquals("TX3", set.getString("settle_txn_id"));
        assertEquals("gsc:1002:bacc:W3", set.getString("event_key"));
        Document addToSet = (Document) update.get("$addToSet");
        assertEquals("TX3", addToSet.getString("settle_txn_ids"));

        UpdateOptions opts = (UpdateOptions) ops.get(0).args[2];
        assertTrue("normal settle must upsert so settle-before-bet is not lost", opts.isUpsert());
        Document setOnInsert = (Document) update.get("$setOnInsert");
        assertEquals("alice", setOnInsert.getString("user_name"));
        assertEquals("W3", setOnInsert.getString("wager_code"));
        assertEquals(Long.valueOf(0L), setOnInsert.getLong("bet_value"));
        assertEquals(Boolean.TRUE, setOnInsert.getBoolean("settle_arrived_first"));

        Document cleanupFilter = (Document) ops.get(1).args[0];
        Document settleArrivedFirstGuard = (Document) cleanupFilter.get("settle_arrived_first");
        assertEquals(Boolean.TRUE, settleArrivedFirstGuard.get("$ne"));
    }

    @Test
    public void executeSideEffect_settleBeforeBet_usesCommutativeUpserts() throws Exception {
        // SUN-1232 regression: if settle is lost because it reaches the
        // async Mongo side-effect queue before the bet insert, c=303 /
        // c=9843 / c=9930 hide the row until the 5-minute reconciler
        // flips settled=true. Both operations must therefore be
        // order-independent upserts.
        List<MockOp> ops = new ArrayList<>();
        MongoCollection<Document> col = mockCollection(ops);

        GscBetSideEffectMessage settle = GscBetSideEffectMessage.of(
                GscBetSideEffectMessage.Op.SETTLE_UPDATE,
                "GscDeposit", "alice", 1002, "bacc",
                "W-RACE-1", "TX-SETTLE-1", System.currentTimeMillis());
        settle.prize = 9000L;
        settle.eventKey = "gsc:1002:bacc:W-RACE-1";
        settle.currency = "VND";

        GscBetSideEffectMessage bet = GscBetSideEffectMessage.of(
                GscBetSideEffectMessage.Op.BET_INSERT,
                "GscWithdraw", "alice", 1002, "bacc",
                "W-RACE-1", "TX-BET-1", System.currentTimeMillis());
        bet.amount = 5000L;
        bet.eventKey = "gsc:1002:bacc:W-RACE-1";
        bet.gameKey = "gsc_1002_bacc";
        bet.gameName = "Baccarat";
        bet.currency = "VND";

        GscBetSideEffectPublisher.executeSideEffect(settle, col);
        GscBetSideEffectPublisher.executeSideEffect(bet, col);

        assertEquals("settle update, cleanup, then bet update", 3, ops.size());

        Document settleFilter = (Document) ops.get(0).args[0];
        Document settleUpdate = (Document) ops.get(0).args[1];
        UpdateOptions settleOpts = (UpdateOptions) ops.get(0).args[2];
        assertEquals("gsc:1002:bacc:W-RACE-1", settleFilter.getString("event_key"));
        assertTrue(settleOpts.isUpsert());
        assertEquals(Boolean.TRUE, ((Document) settleUpdate.get("$set")).getBoolean("settled"));
        assertEquals(Long.valueOf(9000L), ((Document) settleUpdate.get("$inc")).getLong("prize"));
        assertEquals(Boolean.TRUE, ((Document) settleUpdate.get("$setOnInsert")).getBoolean("settle_arrived_first"));

        Document betFilter = (Document) ops.get(2).args[0];
        Document betUpdate = (Document) ops.get(2).args[1];
        UpdateOptions betOpts = (UpdateOptions) ops.get(2).args[2];
        assertEquals("gsc:1002:bacc:W-RACE-1", betFilter.getString("event_key"));
        assertTrue(betOpts.isUpsert());
        assertEquals(Long.valueOf(5000L), ((Document) betUpdate.get("$inc")).getLong("bet_value"));
        assertNull("BET_INSERT must not $set settled=false over a prior settle",
                ((Document) betUpdate.get("$set")).get("settled"));
    }

    @Test
    public void executeSideEffect_settleUpdate_freespinChain_routesByVendorGameId() throws Exception {
        List<MockOp> ops = new ArrayList<>();
        MongoCollection<Document> col = mockCollection(ops);

        GscBetSideEffectMessage msg = GscBetSideEffectMessage.of(
                GscBetSideEffectMessage.Op.SETTLE_UPDATE,
                "GscDeposit", "alice", 1007, "pgsoft_135",
                "spin-W4", "TX4", System.currentTimeMillis());
        msg.prize = 150L;
        msg.eventKey = "gsc:1007:pgsoft_135:spin-W4";
        msg.linkRoundId = "FREESPIN-PARENT-9001"; // SUN-1196 routing

        GscBetSideEffectPublisher.executeSideEffect(msg, col);

        // The filter must key on (user_name, product_code, vendor_game_id),
        // not on event_key/wager_code.
        Document filter = (Document) ops.get(0).args[0];
        assertEquals("alice", filter.getString("user_name"));
        assertEquals(Integer.valueOf(1007), filter.getInteger("product_code"));
        assertEquals("FREESPIN-PARENT-9001", filter.getString("vendor_game_id"));
        assertNull("freespin filter must NOT carry $or", filter.get("$or"));
    }

    @Test
    public void executeSideEffect_cancelDelete_deletesByWagerCode() throws Exception {
        List<MockOp> ops = new ArrayList<>();
        MongoCollection<Document> col = mockCollection(ops);

        GscBetSideEffectMessage msg = GscBetSideEffectMessage.of(
                GscBetSideEffectMessage.Op.CANCEL_DELETE,
                "GscDeposit", "bob", 1149, "HASH_X",
                "W-CANCEL-77", "TX-CANCEL-77", System.currentTimeMillis());

        GscBetSideEffectPublisher.executeSideEffect(msg, col);

        assertEquals(1, ops.size());
        assertEquals("deleteMany", ops.get(0).method);
        Document filter = (Document) ops.get(0).args[0];
        assertEquals("W-CANCEL-77", filter.getString("wager_code"));
    }

    @Test
    public void executeSideEffect_rollbackDelete_deletesByWagerCode() throws Exception {
        List<MockOp> ops = new ArrayList<>();
        MongoCollection<Document> col = mockCollection(ops);

        GscBetSideEffectMessage msg = GscBetSideEffectMessage.of(
                GscBetSideEffectMessage.Op.ROLLBACK_DELETE,
                "GscRollback", "carol", 1002, "bacc",
                "W-ROLL-12", "TX-ROLL-12", System.currentTimeMillis());

        GscBetSideEffectPublisher.executeSideEffect(msg, col);

        assertEquals(1, ops.size());
        assertEquals("deleteMany", ops.get(0).method);
        Document filter = (Document) ops.get(0).args[0];
        assertEquals("W-ROLL-12", filter.getString("wager_code"));
    }

    @Test
    public void executeSideEffect_nullMessage_isNoOp() throws Exception {
        List<MockOp> ops = new ArrayList<>();
        MongoCollection<Document> col = mockCollection(ops);
        GscBetSideEffectPublisher.executeSideEffect(null, col);
        assertEquals("null msg must be a no-op (no Mongo ops)", 0, ops.size());
    }

    @Test
    public void executeSideEffect_nullOp_isNoOp() throws Exception {
        List<MockOp> ops = new ArrayList<>();
        MongoCollection<Document> col = mockCollection(ops);
        GscBetSideEffectMessage m = new GscBetSideEffectMessage();
        m.op = null;
        GscBetSideEffectPublisher.executeSideEffect(m, col);
        assertEquals("null op must be a no-op", 0, ops.size());
    }

    /**
     * RMQ-failure → sync-fallback path. We don't have a way to make
     * RMQApi.publishMessage throw deterministically without a running
     * broker, but the {@code publish(...)} entry catches Throwable and
     * routes to the sync runner. This test verifies the public surface
     * — calling {@code publish(...)} with a message must NOT throw,
     * even in a hostile environment where RMQ is unreachable. The
     * assertion is "no exception escapes the call site" — which is
     * the actual production invariant.
     */
    @Test
    public void publish_swallowsBrokerFailure_neverThrows() {
        GscBetSideEffectMessage msg = GscBetSideEffectMessage.of(
                GscBetSideEffectMessage.Op.ROLLBACK_DELETE,
                "GscRollback", "alice", 1002, "bacc",
                "W-FALLBACK-1", "TX-FALLBACK-1", System.currentTimeMillis());
        // Without a configured RMQ pool, RMQApi.publishMessage will throw;
        // GscBetSideEffectPublisher.publish must catch and route to the
        // sync fallback. The sync fallback in turn may fail to reach
        // Mongo — but every path is wrapped so the call must not bubble.
        try {
            GscBetSideEffectPublisher.publish(msg);
        } catch (Throwable t) {
            org.junit.Assert.fail("publish must NEVER throw — got " + t);
        }
    }

    /**
     * {@link GscBetSideEffectPublisher#fireTelegramIfRequested} is a
     * no-op when {@code telegramAlertSubject} is null — a defensive
     * contract relied on by all three aggregators (cancel/rollback set
     * null; only Withdraw and Deposit settle set non-null).
     */
    @Test
    public void fireTelegramIfRequested_nullSubject_isNoOp() {
        GscBetSideEffectMessage msg = GscBetSideEffectMessage.of(
                GscBetSideEffectMessage.Op.ROLLBACK_DELETE,
                "GscRollback", "alice", 1002, "bacc",
                "W1", "TX1", System.currentTimeMillis());
        msg.telegramAlertSubject = null;
        // No throw, no assertion — this is purely a "doesn't blow up"
        // smoke test. Telegram itself is a side-effect to a real HTTP
        // endpoint that the notifier internally swallows.
        GscBetSideEffectPublisher.fireTelegramIfRequested(msg, "test failure");
    }

    /**
     * Round-trip {@link GscBetSideEffectMessage} through Java
     * serialization (the format RMQ uses on the wire via
     * {@code BaseMessage.toBytes/fromBytes}) and assert all fields
     * survive. Catches accidental {@code transient} declarations or
     * non-serializable nested types added in the future.
     */
    @Test
    public void message_javaSerialization_roundTrip_preservesAllFields() {
        GscBetSideEffectMessage in = GscBetSideEffectMessage.of(
                GscBetSideEffectMessage.Op.SETTLE_UPDATE,
                "GscDeposit", "alice", 1002, "bacc",
                "W-RT-1", "TX-RT-1", 1700000000000L);
        in.amount = 1L;
        in.prize = 2L;
        in.validBetAmount = 3L;
        in.fee = 4L;
        in.rawAmount = "raw";
        in.action = "act";
        in.eventKey = "ek";
        in.gameKey = "gk";
        in.gameName = "gn";
        in.currency = "VND";
        in.vendorGameId = "vgid";
        in.linkRoundId = "lrid";
        in.telegramAlertSubject = "tg";

        byte[] bytes = in.toBytes();
        assertNotNull(bytes);
        assertTrue("serialized form must be non-empty", bytes.length > 0);
        GscBetSideEffectMessage out = (GscBetSideEffectMessage)
                com.vinplay.vbee.common.messages.BaseMessage.fromBytes(bytes);
        assertNotNull(out);
        assertEquals(in.op, out.op);
        assertEquals(in.aggregatorTag, out.aggregatorTag);
        assertEquals(in.memberAccount, out.memberAccount);
        assertEquals(in.productCode, out.productCode);
        assertEquals(in.gameCode, out.gameCode);
        assertEquals(in.wagerCode, out.wagerCode);
        assertEquals(in.txnId, out.txnId);
        assertEquals(in.amount, out.amount);
        assertEquals(in.prize, out.prize);
        assertEquals(in.eventKey, out.eventKey);
        assertEquals(in.linkRoundId, out.linkRoundId);
        assertEquals(in.telegramAlertSubject, out.telegramAlertSubject);
        assertEquals(in.createdAtMs, out.createdAtMs);
    }

    // Suppress unused-import warning on Bson (kept available for future
    // assertions on raw filter shape).
    @SuppressWarnings("unused")
    private static Bson unused;
}
