package com.vinplay.dal.service.seamless.gsc;

import com.vinplay.dal.service.MoneyGateway;
import com.vinplay.dal.service.seamless.SeamlessOutcome;
import com.vinplay.vbee.common.config.VBeePath;
import com.vinplay.vbee.common.messages.GscBetSideEffectMessage;
import com.vinplay.vbee.common.messages.LogMoneyUserMessage;
import com.vinplay.vbee.common.models.cache.UserCacheModel;
import com.vinplay.vbee.common.pools.ConnectionPool;
import com.vinplay.vbee.common.statics.Consts;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.AfterClass;
import org.junit.Assume;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import javax.servlet.http.HttpServletRequest;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Iterator;
import java.util.Properties;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/**
 * Phase 3f golden-file contract test for {@link GscWithdrawAggregator}.
 *
 * <p>Each fixture pair under
 * {@code src/test/resources/seamless/gsc/withdraw/NN_request.json} +
 * {@code NN_response.json} is replayed through the aggregator with a
 * stub {@link GscConfigProvider} that uses a known secret
 * ({@code TEST_SECRET}). The aggregator's response is compared to the
 * expected payload as <em>JSON-tree-equivalent</em> — same comparison
 * helper as {@link GscDepositAggregatorTest} and earlier phases.
 *
 * <h2>Test-secret-vs-production-secret asymmetry</h2>
 * Production payloads from {@code gsc_event_log} were signed with the
 * operator's real shared secret (not exposed here). Replaying them
 * through the aggregator with {@code TEST_SECRET} therefore predictably
 * trips the signature check, returning the SIGNATURE_ERROR shape. That
 * is precisely the point: production fixtures verify the parser
 * doesn't crash on real-world inputs (extreme nesting, batch_requests
 * envelope, multi-byte UUIDs, negative-amount BET sign convention),
 * and the SIGNATURE_ERROR shape is the documented expected output for
 * "valid JSON but bad sign". Synthetic fixtures (04+) carry signatures
 * computed against {@code TEST_SECRET} so they reach the dispatch path
 * and exercise the validation branches.
 *
 * <h2>Fixture map</h2>
 * <ul>
 *   <li>{@code 01} — Real production payload (Evolution 1002 BET, KRW,
 *       batch_requests envelope, negative-amount sign convention,
 *       Blackjack-style payload "D&lt;round&gt;" nested map). Bad sign vs
 *       TEST_SECRET → SIGNATURE_ERROR.
 *       Sourced from {@code vinplay.gsc_event_log WHERE
 *       gsc_endpoint='withdraw' AND product_code=1002}.</li>
 *   <li>{@code 02} — Real production payload (PG Soft 1007 BET, VND2 with
 *       1000:1 fx, parent_round_id freespin chain link). Bad sign →
 *       SIGNATURE_ERROR. Sourced from gsc_event_log product_code=1007.</li>
 *   <li>{@code 03} — Real production payload (Hash Game 1149 BET, VND,
 *       HASH_BACCARAT, weird payload with agent_token + id-keyed nested
 *       map). Bad sign → SIGNATURE_ERROR. Sourced from gsc_event_log
 *       product_code=1149.</li>
 *   <li>{@code 04} — Valid sign, normal BET (Evolution KRW). Member missing
 *       in DB+Hazelcast → batch aborts via doDebit USER_NOT_FOUND →
 *       response {@code {"code":999,"message":"Server Error","data":null}}.
 *       Confirms the dispatch path reaches doDebit.</li>
 *   <li>{@code 05} — Valid sign, Dream Gaming HEDGE bet (1052 VND,
 *       valid_bet_amount=10000 != bet_amount=50000, the SUN-1205/1206
 *       case). Member missing → same 999 path. Confirms the dispatch
 *       path on a hedge-bet payload — the rebate-publish capture test
 *       below exercises the actual validBetAmount injection.</li>
 *   <li>{@code 06} — Valid sign, unknown currency "ZZZ" → empty
 *       BalanceResponse fall-back: {@code {"code":0,"message":"","data":[]}}.</li>
 *   <li>{@code 07} — Valid sign, missing required parameter
 *       (member_account omitted entirely; no batch_requests fall-back)
 *       → {@code {"code":1002,"message":"Missing required parameters","data":null}}.</li>
 *   <li>{@code 08} — Bad sign with otherwise valid required-params →
 *       SIGNATURE_ERROR shape: {@code {"code":1004,"message":"Invalid sign","data":null}}.</li>
 * </ul>
 *
 * <h2>Direction coverage</h2>
 * {@link #mapActionToSourceAlwaysDebit} asserts {@code mapActionToSource}
 * returns {@link MoneyGateway#SOURCE_GSC_DEBIT} regardless of action verb
 * — defense against accidental future direction switch.
 *
 * <h2>Rebate publish capture</h2>
 * {@link #afterDebit_normalBet_publishesRebateMessage} subclasses the
 * aggregator overriding {@code publishRebateMessage} to capture the
 * message, plus overriding {@code resolveUserIdForRebate} /
 * {@code lookupUserCache} to bypass the DB and Hazelcast (so the test
 * runs unconditionally — no Assume on bootstrapSkipped). Invokes
 * {@code afterDebit} directly via reflection. Asserts every field
 * value: userId, nickname, actionName, serviceName, currentMoney,
 * moneyExchange (NEGATIVE for debit), moneyType, fee, vp, isBot,
 * source-key.
 *
 * <h2>SUN-1205/1206 hedge-bet validBetAmount injection</h2>
 * {@link #afterDebit_hedgeBet_setsValidBetAmount} confirms that when a
 * provider's {@code resolveCommissionVolume} returns a value DIFFERENT
 * from the deducted amount, the published {@link LogMoneyUserMessage}
 * carries that smaller value via {@link LogMoneyUserMessage#setValidBetAmount}.
 *
 * <h2>SUN-980 commission-ineligible transfer-in</h2>
 * {@link #afterDebit_cq9FishTransfer_usesSentinelActionName} confirms
 * the {@code "Cq9FishTransfer"} sentinel action is set in the rebate
 * message when the transfer-in flag fires (so
 * {@code RealTimeCommission.EXCLUDED_ACTIONS} skips it downstream).
 */
public class GscWithdrawAggregatorTest {

    private static final String TEST_SECRET = "TEST_SECRET";
    private static volatile boolean bootstrapSkipped = false;

    private GscWithdrawAggregator aggregator;

    @BeforeClass
    public static void bootstrapPool() throws Exception {
        String pw = System.getProperty("test.mysql.password");
        if (pw == null || pw.isEmpty()) pw = System.getenv("MYSQL_PASSWORD");
        if (pw == null || pw.isEmpty()) {
            bootstrapSkipped = true;
            System.out.println("GscWithdrawAggregatorTest: password not supplied; tests will be skipped");
            return;
        }

        URL propsUrl = GscWithdrawAggregatorTest.class
                .getClassLoader()
                .getResource("config/db_pool.properties");
        String resourcesDir = propsUrl.getPath().replace("config/db_pool.properties", "");
        VBeePath.basePath = resourcesDir;

        Properties props = new Properties();
        try (InputStream is = propsUrl.openStream()) {
            props.load(is);
        }
        final String PLACEHOLDER = "__INJECT_AT_TEST_TIME__";
        if (PLACEHOLDER.equals(props.getProperty("mysqlpoolname.password", ""))) {
            props.setProperty("mysqlpoolname.password", pw);
        }
        java.io.File propsFile = new java.io.File(propsUrl.toURI());
        try (java.io.FileOutputStream fos = new java.io.FileOutputStream(propsFile)) {
            props.store(fos, "Auto-generated by GscWithdrawAggregatorTest — password injected at test time");
        }

        ConnectionPool.getInstance();
    }

    @AfterClass
    public static void cleanUp() {
        if (bootstrapSkipped) return;
        try {
            URL propsUrl = GscWithdrawAggregatorTest.class
                    .getClassLoader()
                    .getResource("config/db_pool.properties");
            if (propsUrl != null) {
                Properties props = new Properties();
                try (InputStream is = propsUrl.openStream()) {
                    props.load(is);
                }
                props.setProperty("mysqlpoolname.password", "__INJECT_AT_TEST_TIME__");
                java.io.File propsFile = new java.io.File(propsUrl.toURI());
                try (java.io.FileOutputStream fos = new java.io.FileOutputStream(propsFile)) {
                    props.store(fos,
                            "Integration-test DB pool — password placeholder restored after run");
                }
            }
        } catch (Exception e) {
            System.err.println("GscWithdrawAggregatorTest: could not restore placeholder (non-fatal): "
                    + e.getMessage());
        }
    }

    @Before
    public void setUp() {
        aggregator = new GscWithdrawAggregator(stubConfig(),
                GscWithdrawProviderHooks.Resolver.DEFAULT);
    }

    // ─────────────────────────────────────────────────────────────────
    // Fixtures
    // ─────────────────────────────────────────────────────────────────

    @Test public void fixture01_productionEvolutionBet_signatureError()  throws Exception { runFixture("01"); }
    @Test public void fixture02_productionPgSoftFreespin_signatureError() throws Exception { runFixture("02"); }
    @Test public void fixture03_productionHashGameBet_signatureError()    throws Exception { runFixture("03"); }

    /**
     * Fixtures 04 and 05 reach the wallet primitives and therefore
     * require the DB pool to be available; the rest short-circuit
     * before any {@code doDebit} call and run unconditionally.
     */
    @Test public void fixture04_validSig_normalBet_userMissing() throws Exception {
        Assume.assumeFalse("DB password not supplied", bootstrapSkipped);
        runFixture("04");
    }
    @Test public void fixture05_validSig_dreamGamingHedgeBet_userMissing() throws Exception {
        Assume.assumeFalse("DB password not supplied", bootstrapSkipped);
        runFixture("05");
    }
    @Test public void fixture06_unknownCurrency_balanceFallback() throws Exception { runFixture("06"); }
    @Test public void fixture07_missingRequiredParameters()       throws Exception { runFixture("07"); }
    @Test public void fixture08_invalidSignature_validFields()    throws Exception { runFixture("08"); }

    /**
     * Replay 3 raw production-shape payloads sourced directly from
     * {@code vinplay.gsc_event_log WHERE gsc_endpoint='withdraw'} and
     * verify the parser doesn't choke or crash. Production sigs are
     * computed against the real operator secret, not {@link #TEST_SECRET},
     * so the aggregator returns the SIGNATURE_ERROR shape on each — which
     * verifies the no-crash invariant + the "Invalid sign" response shape
     * under realistic input nesting (Evolution Blackjack payload, PG Soft
     * parent_round_id, JILI fish payload).
     */
    @Test
    public void productionPayloadsParseWithoutThrowing() throws Exception {
        String[] payloads = {
                // Evolution 1002 — KRW, batch_requests, "D<round>" nested payload, BET amount=-2000
                "{\"sign\":\"b0426d9aaf9c078ccc7c0960e715f4c5\",\"currency\":\"KRW\",\"game_type\":\"\",\"request_time\":\"1777720752\",\"operator_code\":\"G7A1\",\"batch_requests\":[{\"game_type\":\"LIVE_CASINO\",\"product_code\":1002,\"transactions\":[{\"id\":\"770e73d4-ed6e-4ab2-8cfb-8d81d7385853\",\"action\":\"BET\",\"amount\":\"-2000\",\"payload\":{\"Dd3f1c480-84c7-4ded-ae0e-3f9246722983\":{\"game_id\":\"18abbb97c4742c7ea871d973-txc4bi5gfh5r7jqg\",\"session_id\":\"Si2szX6FLPCkZNcf2zpSgV\",\"game_table_id\":\"ndgvwvgthfuaad3q\",\"game_table_vid\":\"\"}},\"round_id\":\"d3f1c480-84c7-4ded-ae0e-3f9246722983\",\"game_code\":\"ndgvwvgthfuaad3q\",\"bet_amount\":\"2000\",\"wager_code\":\"GtoFXH7EPvUwRyu3H58GXn\",\"prize_amount\":\"0\",\"wager_status\":\"BET\",\"valid_bet_amount\":\"2000\"}],\"member_account\":\"bunbohue02\"}]}",
                // PG Soft 1007 — VND2, parent_round_id freespin chain
                "{\"sign\":\"fb6fc86855485739133c9f876865fdc0\",\"currency\":\"VND2\",\"game_type\":\"\",\"request_time\":\"1777695149\",\"operator_code\":\"G7A1\",\"batch_requests\":[{\"game_type\":\"SLOT\",\"product_code\":1007,\"transactions\":[{\"id\":\"3f156ab0-6a5e-4424-85ab-584eb6b33938\",\"action\":\"BET\",\"amount\":\"-150\",\"payload\":{\"2050428185736258053\":{\"amount\":\"150\",\"action_type\":\"BET_SETTLED\",\"parent_round_id\":\"2050428185736258053\"}},\"round_id\":\"2050428185736258053\",\"game_code\":\"135\",\"bet_amount\":\"150\",\"wager_code\":\"qBDkWKYGupPts2EFxaYKon\",\"prize_amount\":\"0\",\"wager_status\":\"BET\",\"valid_bet_amount\":\"150\"}],\"member_account\":\"quochuy98\"}]}",
                // JILI 1091 — VND, FISHING with simpler payload shape
                "{\"sign\":\"d064b54bd783698b16cfff6c0058ee91\",\"currency\":\"VND\",\"game_type\":\"\",\"request_time\":\"1777720648\",\"operator_code\":\"G7A1\",\"batch_requests\":[{\"game_type\":\"FISHING\",\"product_code\":1091,\"transactions\":[{\"id\":\"3fbe7b70-4073-4433-a8ee-948f7e8bf371\",\"action\":\"BET\",\"amount\":\"-900\",\"payload\":{\"token\":\"wHjCrwoT7RBAgUEiNxcnUd\",\"bet_id\":\"617722071656581856\",\"parent_bet_id\":null,\"provider_tx_id\":\"7075491710832370996\"},\"round_id\":\"617722071656581856\",\"game_code\":\"736\",\"bet_amount\":\"900\",\"wager_code\":\"WyiAEjft47Zz3oQP3L9yp7\",\"prize_amount\":\"0\",\"wager_status\":\"BET\",\"valid_bet_amount\":\"900\"}],\"member_account\":\"bunbohue02\"}]}"
        };
        for (String body : payloads) {
            String resp = aggregator.handle(mockRequest(body));
            assertNotNull(resp);
            JSONObject obj = new JSONObject(resp);
            assertEquals(1004, obj.optInt("code"));
            assertEquals("Invalid sign", obj.optString("message"));
            // Withdraw failure shape: data is null, NOT a list (different from Deposit)
            assertTrue("data must be null on error, was=" + obj.opt("data"),
                    obj.isNull("data"));
        }
    }

    /**
     * Direction-coverage smoke test: every action verb on the withdraw
     * endpoint maps to {@link MoneyGateway#SOURCE_GSC_DEBIT}. Defensive
     * against an accidental future direction switch.
     */
    @Test
    public void mapActionToSourceAlwaysDebit() throws Exception {
        Method m = GscWithdrawAggregator.class.getDeclaredMethod(
                "mapActionToSource", String.class);
        m.setAccessible(true);
        String[] actions = {"BET", "bet", "SETTLE", "settle",
                            "CANCEL", "cancel", "BONUS", "bonus",
                            null, "", "garbage_action"};
        for (String a : actions) {
            Object got = m.invoke(aggregator, a);
            assertEquals("action=" + a + " must map to SOURCE_GSC_DEBIT",
                    MoneyGateway.SOURCE_GSC_DEBIT, got);
        }
    }

    /**
     * Phase 3e rebate-publish gap-fix carried to 3f: when {@code afterDebit}
     * runs for a normal BET outcome, it must publish a
     * {@link LogMoneyUserMessage} to {@code queue_log_money_user_extra} so
     * the agency-commission pipeline ({@code LogMoneyUserExtraProcessor})
     * generates the per-bet rebate rows. Mirrors what
     * {@code userMoneyService.bet} → {@code UserServiceImpl.updateMoney}
     * does on the legacy path.
     *
     * <p>{@link MoneyGateway#debitUser} is a pure wallet primitive and
     * does NOT publish the rebate event — without this hook the agency
     * commission generation silently breaks on flag-on traffic.
     *
     * <p>moneyExchange must be NEGATIVE for the debit direction (mirrors
     * legacy reward-vs-bet directions on UserMoneyServiceImpl).
     */
    @Test
    public void afterDebit_normalBet_publishesRebateMessage() throws Exception {
        final java.util.concurrent.atomic.AtomicReference<LogMoneyUserMessage> captured =
                new java.util.concurrent.atomic.AtomicReference<>();
        GscWithdrawAggregator agg = new GscWithdrawAggregator(
                stubConfig(), GscWithdrawProviderHooks.Resolver.DEFAULT) {
            @Override
            protected void publishRebateMessage(LogMoneyUserMessage msg) {
                captured.set(msg);
            }
            @Override
            protected UserCacheModel lookupUserCache(String memberAccount) {
                return null; // skip Hazelcast — exercise the DB-fallback isBot=false path
            }
            @Override
            protected int resolveUserIdForRebate(String memberAccount, UserCacheModel cached) {
                return 4242; // bypass DB lookup
            }
        };

        GscRequest.TransactionItem t = new GscRequest.TransactionItem(
                "TX-W-12345", "BET", "alice", 1002, "bacc_lobby_1",
                /*currency*/ "VND", /*wagerCode*/ "W-W-12345",
                /*amount*/ "-5000", /*betAmount*/ "5000",
                /*wagerStatus*/ "BET", /*prizeAmount*/ "0",
                /*validBetAmount*/ "5000", /*roundId*/ "round-1",
                /*payload*/ null);
        java.util.List<GscRequest.TransactionItem> txns = new java.util.ArrayList<>();
        txns.add(t);
        GscRequest req = new GscRequest(
                /*operatorCode*/ "G7A1", /*currency*/ "VND",
                /*sign*/ "ignored", /*requestTime*/ "0",
                /*memberAccount*/ "alice", /*productCode*/ 1002,
                /*gameType*/ "LIVE_CASINO",
                /*batchMembers*/ null, txns);

        invokeAfterDebit(agg, req, t,
                GscWithdrawProviderHooks.DEFAULT,
                /*isCq9FishTransfer*/ false,
                /*amountSubunit*/ 5000L,
                /*validBetAmt*/ 5000d,
                /*betAmt*/ 5000d,
                /*fxIn*/ 1d,
                /*postDebitBalance*/ 95000L);

        LogMoneyUserMessage msg = captured.get();
        assertNotNull("publishRebateMessage must be invoked on POSTED", msg);
        assertEquals("userId", 4242, msg.getUserId());
        assertEquals("nickname", "alice", msg.getNickname());
        assertEquals("actionName / gameName", "gsc_1002_bacc_lobby_1", msg.getActionName());
        assertEquals("serviceName == String.valueOf(productCode)", "1002", msg.getServiceName());
        assertEquals("currentMoney = post-debit balance", 95000L, msg.getCurrentMoney());
        assertEquals("moneyExchange — NEGATIVE for debit", -5000L, msg.getMoneyExchange());
        assertEquals("moneyType", Consts.MONEY_VIN, msg.getMoneyType());
        assertEquals("fee — taxPercent=0 → 0", 0L, msg.getFee());
        assertTrue("vp / playgame must be true",  msg.isVp());
        assertTrue("isBot=false when Hazelcast cache is empty", !msg.isBot());
        assertEquals("stable source-key for at-least-once dedup",
                "gsc:TX-W-12345", msg.getId());
        // commissionVolume == amount → validBetAmount stays at 0 (default).
        assertEquals("validBetAmount=0 when commissionVolume == amount",
                0L, msg.getValidBetAmount());
    }

    /**
     * SUN-1205/1206 hedge-bet validBetAmount injection. When the provider
     * returns a {@code resolveCommissionVolume} that differs from the
     * deducted amount, the rebate message must carry that smaller value
     * via {@link LogMoneyUserMessage#setValidBetAmount} so the rebate
     * pipeline reads the smaller volume for commission rate calculation.
     * Money flow (vin debit) stays at the larger amount.
     */
    @Test
    public void afterDebit_hedgeBet_setsValidBetAmount() throws Exception {
        final java.util.concurrent.atomic.AtomicReference<LogMoneyUserMessage> captured =
                new java.util.concurrent.atomic.AtomicReference<>();
        // Provider hook returning the smaller commission volume. Mirrors
        // DreamGamingProvider's default behavior on hedge bets where
        // valid_bet_amount=10000 < bet_amount=50000.
        GscWithdrawProviderHooks hedgeProvider = new GscWithdrawProviderHooks() {
            @Override
            public long resolveCommissionVolume(double validBetAmount, double betAmount,
                                                long deductAmount, double fxIn) {
                return Math.round(Math.abs(validBetAmount) * fxIn);
            }
        };
        GscWithdrawAggregator agg = new GscWithdrawAggregator(
                stubConfig(), GscWithdrawProviderHooks.Resolver.DEFAULT) {
            @Override
            protected void publishRebateMessage(LogMoneyUserMessage msg) {
                captured.set(msg);
            }
            @Override
            protected UserCacheModel lookupUserCache(String memberAccount) { return null; }
            @Override
            protected int resolveUserIdForRebate(String memberAccount, UserCacheModel cached) {
                return 4242;
            }
        };

        // Hedge bet: bet=50000, valid_bet=10000. Wallet debits 50000;
        // rebate pipeline must see 10000 via validBetAmount.
        GscRequest.TransactionItem t = new GscRequest.TransactionItem(
                "TX-W-HEDGE-1", "BET", "alice", 1052, "BACCARAT_A01",
                "VND", "W-W-HEDGE-1",
                "-50000", "50000",
                "BET", "0",
                "10000", "round-hedge",
                null);
        java.util.List<GscRequest.TransactionItem> txns = new java.util.ArrayList<>();
        txns.add(t);
        GscRequest req = new GscRequest(
                "G7A1", "VND", "ignored", "0",
                "alice", 1052, "LIVE_CASINO", null, txns);

        invokeAfterDebit(agg, req, t, hedgeProvider,
                /*isCq9FishTransfer*/ false,
                /*amountSubunit*/ 50000L,
                /*validBetAmt*/ 10000d,
                /*betAmt*/ 50000d,
                /*fxIn*/ 1d,
                /*postDebitBalance*/ 50000L);

        LogMoneyUserMessage msg = captured.get();
        assertNotNull("publishRebateMessage must be invoked on POSTED", msg);
        // Wallet flow stays at the deducted amount (negative for debit).
        assertEquals("moneyExchange — full debit amount (NEGATIVE)",
                -50000L, msg.getMoneyExchange());
        // SUN-1205/1206 — validBetAmount carries the smaller commission volume.
        assertEquals("validBetAmount — smaller commission volume",
                10000L, msg.getValidBetAmount());
        assertEquals("actionName for Dream live-casino game",
                "gsc_1052_BACCARAT_A01", msg.getActionName());
    }

    /**
     * SUN-980: when the (product_code, game_code) pair is marked
     * commission_eligible=0 on gsc_game_catalog AND both
     * valid_bet_amount and bet_amount are zero, the rebate message
     * must use the {@code "Cq9FishTransfer"} sentinel action so the
     * downstream {@code RealTimeCommission.EXCLUDED_ACTIONS} skips it.
     * Wallet still debits.
     */
    @Test
    public void afterDebit_cq9FishTransfer_usesSentinelActionName() throws Exception {
        final java.util.concurrent.atomic.AtomicReference<LogMoneyUserMessage> captured =
                new java.util.concurrent.atomic.AtomicReference<>();
        GscWithdrawAggregator agg = new GscWithdrawAggregator(
                stubConfig(), GscWithdrawProviderHooks.Resolver.DEFAULT) {
            @Override
            protected void publishRebateMessage(LogMoneyUserMessage msg) {
                captured.set(msg);
            }
            @Override
            protected UserCacheModel lookupUserCache(String memberAccount) { return null; }
            @Override
            protected int resolveUserIdForRebate(String memberAccount, UserCacheModel cached) {
                return 4242;
            }
        };

        // CQ9 fish transfer-in: 0/0 amounts, no wager_code. Wallet
        // would still debit `amount` but rebate publish must use sentinel.
        GscRequest.TransactionItem t = new GscRequest.TransactionItem(
                "TX-W-FISH-1", "BET", "alice", 1085, "fish_thunder",
                "VND", /*wagerCode*/ "",
                "-1000", "0",
                "BET", "0",
                "0", "round-fish",
                null);
        java.util.List<GscRequest.TransactionItem> txns = new java.util.ArrayList<>();
        txns.add(t);
        GscRequest req = new GscRequest(
                "G7A1", "VND", "ignored", "0",
                "alice", 1085, "FISHING", null, txns);

        invokeAfterDebit(agg, req, t,
                GscWithdrawProviderHooks.DEFAULT,
                /*isCq9FishTransfer*/ true,
                /*amountSubunit*/ 1000L,
                /*validBetAmt*/ 0d,
                /*betAmt*/ 0d,
                /*fxIn*/ 1d,
                /*postDebitBalance*/ 99000L);

        LogMoneyUserMessage msg = captured.get();
        assertNotNull("publishRebateMessage must still publish (consumer filters via EXCLUDED_ACTIONS)", msg);
        assertEquals("actionName must be the Cq9FishTransfer sentinel",
                "Cq9FishTransfer", msg.getActionName());
    }

    /**
     * Phase 5 prep gate 5p2 — afterDebit must publish exactly one
     * {@link GscBetSideEffectMessage} carrying the legacy log_gsc_bets
     * insert payload. The wallet debit stays synchronous (verified
     * elsewhere); this asserts the post-wallet bookkeeping moves to
     * the RMQ path. Telegram alert subject MUST be the legacy wording
     * ("withdraw insertOne") so the consumer's alert path matches the
     * pre-async behavior byte-for-byte.
     */
    @Test
    public void afterDebit_normalBet_publishesBetSideEffect() throws Exception {
        final java.util.concurrent.atomic.AtomicReference<GscBetSideEffectMessage> sfx =
                new java.util.concurrent.atomic.AtomicReference<>();
        final java.util.concurrent.atomic.AtomicInteger sfxCount =
                new java.util.concurrent.atomic.AtomicInteger();
        GscWithdrawAggregator agg = new GscWithdrawAggregator(
                stubConfig(), GscWithdrawProviderHooks.Resolver.DEFAULT) {
            @Override
            protected void publishRebateMessage(LogMoneyUserMessage msg) { /* swallow */ }
            @Override
            protected void publishBetSideEffect(GscBetSideEffectMessage msg) {
                sfx.set(msg);
                sfxCount.incrementAndGet();
            }
            @Override
            protected UserCacheModel lookupUserCache(String memberAccount) { return null; }
            @Override
            protected int resolveUserIdForRebate(String memberAccount, UserCacheModel cached) { return 1; }
        };

        GscRequest.TransactionItem t = new GscRequest.TransactionItem(
                "TX-W-SFX-1", "BET", "alice", 1002, "bacc_lobby_1",
                "VND", "W-W-SFX-1", "-5000", "5000",
                "BET", "0", "5000", "round-1", null);
        java.util.List<GscRequest.TransactionItem> txns = new java.util.ArrayList<>();
        txns.add(t);
        GscRequest req = new GscRequest(
                "G7A1", "VND", "ignored", "0",
                "alice", 1002, "LIVE_CASINO", null, txns);

        invokeAfterDebit(agg, req, t,
                GscWithdrawProviderHooks.DEFAULT,
                /*isCq9FishTransfer*/ false,
                /*amountSubunit*/ 5000L,
                /*validBetAmt*/ 5000d,
                /*betAmt*/ 5000d,
                /*fxIn*/ 1d,
                /*postDebitBalance*/ 95000L);

        assertEquals("publishBetSideEffect must fire exactly once on POSTED",
                1, sfxCount.get());
        GscBetSideEffectMessage m = sfx.get();
        assertNotNull(m);
        assertEquals("op", GscBetSideEffectMessage.Op.BET_INSERT, m.op);
        assertEquals("aggregatorTag", "GscWithdraw", m.aggregatorTag);
        assertEquals("memberAccount", "alice", m.memberAccount);
        assertEquals("productCode", 1002, m.productCode);
        assertEquals("gameCode", "bacc_lobby_1", m.gameCode);
        assertEquals("wagerCode", "W-W-SFX-1", m.wagerCode);
        assertEquals("txnId", "TX-W-SFX-1", m.txnId);
        assertEquals("amount", 5000L, m.amount);
        assertEquals("currency", "VND", m.currency);
        assertEquals("gameKey carries rebate action_name",
                "gsc_1002_bacc_lobby_1", m.gameKey);
        assertEquals("telegramAlertSubject — legacy wording byte-for-byte",
                "withdraw insertOne", m.telegramAlertSubject);
        assertTrue("createdAtMs must be set", m.createdAtMs > 0L);
    }

    /**
     * SUN-980 transfer-in: when the catalog says commission-ineligible
     * AND amounts are 0/0, the aggregator must skip the side-effect
     * publish entirely (legacy parity — no log_gsc_bets row written).
     */
    @Test
    public void afterDebit_cq9FishTransfer_skipsBetSideEffectPublish() throws Exception {
        final java.util.concurrent.atomic.AtomicInteger sfxCount =
                new java.util.concurrent.atomic.AtomicInteger();
        GscWithdrawAggregator agg = new GscWithdrawAggregator(
                stubConfig(), GscWithdrawProviderHooks.Resolver.DEFAULT) {
            @Override protected void publishRebateMessage(LogMoneyUserMessage msg) { /* swallow */ }
            @Override
            protected void publishBetSideEffect(GscBetSideEffectMessage msg) {
                sfxCount.incrementAndGet();
            }
            @Override protected UserCacheModel lookupUserCache(String memberAccount) { return null; }
            @Override
            protected int resolveUserIdForRebate(String memberAccount, UserCacheModel cached) { return 1; }
        };

        GscRequest.TransactionItem t = new GscRequest.TransactionItem(
                "TX-W-FISH-2", "BET", "alice", 1085, "fish_thunder",
                "VND", "", "-1000", "0",
                "BET", "0", "0", "round-fish", null);
        java.util.List<GscRequest.TransactionItem> txns = new java.util.ArrayList<>();
        txns.add(t);
        GscRequest req = new GscRequest("G7A1", "VND", "ignored", "0",
                "alice", 1085, "FISHING", null, txns);

        invokeAfterDebit(agg, req, t,
                GscWithdrawProviderHooks.DEFAULT,
                /*isCq9FishTransfer*/ true,
                /*amountSubunit*/ 1000L,
                /*validBetAmt*/ 0d, /*betAmt*/ 0d, /*fxIn*/ 1d,
                /*postDebitBalance*/ 99000L);

        assertEquals("CQ9 fish transfer-in must NOT publish side effect",
                0, sfxCount.get());
    }

    // ─────────────────────────────────────────────────────────────────
    // SUN-1373 — GAME_INACTIVE path tests
    // ─────────────────────────────────────────────────────────────────

    /**
     * SUN-1373 — {@link SeamlessOutcome#gameInactive()} must carry status
     * {@link SeamlessOutcome.Status#GAME_INACTIVE}, errorCode {@code "0011"},
     * and message {@code "GAME_INACTIVE"}. No wallet movement ({@code newBalance=0}).
     */
    @Test
    public void gameInactiveOutcome_hasCorrectFields() {
        SeamlessOutcome out = SeamlessOutcome.gameInactive();
        assertEquals("status must be GAME_INACTIVE",
                SeamlessOutcome.Status.GAME_INACTIVE, out.status);
        assertEquals("errorCode must be 0011", "0011", out.errorCode);
        assertEquals("errorMessage must be GAME_INACTIVE", "GAME_INACTIVE", out.errorMessage);
        assertEquals("newBalance must be 0 — no wallet movement", 0L, out.newBalance);
    }

    /**
     * SUN-1373 — when the aggregator's {@code serializeResponse} receives a
     * {@code GAME_INACTIVE} outcome it must return the GSC withdraw-error
     * JSON shape with code {@code 1005} and message {@code "GAME_INACTIVE"}.
     *
     * <p>Uses reflection on the {@code protected serializeResponse} method
     * so we can call it directly without going through the full HTTP pipeline
     * (avoids needing Hazelcast or DB for this unit test).
     */
    @Test
    public void serializeResponse_gameInactive_returnsCode1005() throws Exception {
        Method m = GscWithdrawAggregator.class.getDeclaredMethod(
                "serializeResponse", SeamlessOutcome.class);
        m.setAccessible(true);

        SeamlessOutcome inactiveOutcome = SeamlessOutcome.gameInactive();
        String json = (String) m.invoke(aggregator, inactiveOutcome);

        assertNotNull("serializeResponse must not return null", json);
        JSONObject obj = new JSONObject(json);
        assertEquals("code must be 1005 (SC_GAME_INACTIVE)", 1005, obj.optInt("code"));
        assertEquals("message must be GAME_INACTIVE", "GAME_INACTIVE", obj.optString("message"));
        // GSC error shape: data must be null (same as other error codes)
        assertTrue("data must be null on GAME_INACTIVE error, was=" + obj.opt("data"),
                obj.isNull("data"));
    }

    /**
     * SUN-1373 — {@code GscGameNameResolver.isGameActive} must return
     * {@code true} (fail-open) when there is no DB row for the given
     * (product_code, game_code). This guards against the gate blocking ALL
     * bets when the catalog is not yet seeded.
     *
     * <p>Runs unconditionally — no DB required. When the DB pool is not
     * available {@code lookupCatalogColumn} catches the exception and returns
     * the default value ({@code Boolean.TRUE}), so the method returns
     * {@code true}. This is the same fail-open posture as
     * {@code isCommissionEligible}.
     */
    @Test
    public void isGameActive_missingRow_returnsTrueFailOpen() {
        // No DB bootstrapped in this test → connection will fail → default=TRUE
        // (the cache miss + exception path both return the defaultValue)
        boolean active = com.vinplay.dal.service.GscGameNameResolver.isGameActive(99999, "NO_SUCH_GAME");
        assertTrue("isGameActive must fail-open (return true) when catalog row is absent or DB unavailable",
                active);
    }

    /**
     * SUN-1373 — {@code GscGameNameResolver.isGameActive} must return
     * {@code true} when called with null game_code (treated as empty string,
     * which won't match any real catalog row → fail-open).
     */
    @Test
    public void isGameActive_nullGameCode_returnsTrueFailOpen() {
        boolean active = com.vinplay.dal.service.GscGameNameResolver.isGameActive(1002, null);
        assertTrue("isGameActive must fail-open when gameCode is null", active);
    }

    /** Reflection helper — afterDebit is private. */
    private static void invokeAfterDebit(GscWithdrawAggregator agg,
                                         GscRequest req,
                                         GscRequest.TransactionItem t,
                                         GscWithdrawProviderHooks provider,
                                         boolean isCq9FishTransfer,
                                         long amountSubunit,
                                         double validBetAmt,
                                         double betAmt,
                                         double fxIn,
                                         long postDebitBalance) throws Exception {
        Method m = GscWithdrawAggregator.class.getDeclaredMethod(
                "afterDebit",
                GscRequest.class, GscRequest.TransactionItem.class,
                GscWithdrawProviderHooks.class, boolean.class,
                long.class, double.class, double.class, double.class, long.class);
        m.setAccessible(true);
        m.invoke(agg, req, t, provider, isCq9FishTransfer,
                amountSubunit, validBetAmt, betAmt, fxIn, postDebitBalance);
    }

    /** Same stub config as the {@link #setUp} fixture aggregator. */
    private static GscConfigProvider stubConfig() {
        return new GscConfigProvider() {
            @Override public String getSecretKey() { return TEST_SECRET; }
            @Override public double getOperatorExchangeRate() { return 1.0; }
            @Override
            public int getCurrencyExchangeRate(String currencyCode) {
                if (currencyCode == null) return 0;
                String s = currencyCode.trim();
                if ("KRW".equals(s) || "VND".equals(s) || "USD".equals(s) || "CNY".equals(s)
                        || "IDR".equals(s) || "MYR".equals(s) || "JPY".equals(s)
                        || "THB".equals(s) || "SGD".equals(s)) {
                    return 1;
                }
                if ("KRW2".equals(s) || "VND2".equals(s) || "IDR2".equals(s)
                        || "MMK2".equals(s) || "LAK2".equals(s) || "KHR2".equals(s)) {
                    return 1000;
                }
                return 0;
            }
            @Override public int getTaxPercent() { return 0; }
        };
    }

    // ─────────────────────────────────────────────────────────────────
    // Helpers (mirror GscDepositAggregatorTest)
    // ─────────────────────────────────────────────────────────────────

    private void runFixture(String name) throws Exception {
        String reqPath = "seamless/gsc/withdraw/" + name + "_request.json";
        String respPath = "seamless/gsc/withdraw/" + name + "_response.json";
        String reqBody = readResource(reqPath);
        String expectedRaw = readResource(respPath);
        assertNotNull("missing fixture " + reqPath, reqBody);
        assertNotNull("missing fixture " + respPath, expectedRaw);

        JSONObject expected = new JSONObject(expectedRaw);

        String actualJson = aggregator.handle(mockRequest(reqBody));
        assertNotNull("aggregator returned null for " + name, actualJson);
        JSONObject actual = new JSONObject(actualJson);

        assertJsonEquals("fixture " + name + ": response mismatch.\n  expected="
                + expected + "\n  actual=" + actual, expected, actual);
    }

    private static String readResource(String path) throws Exception {
        try (InputStream is = GscWithdrawAggregatorTest.class
                .getClassLoader().getResourceAsStream(path)) {
            if (is == null) return null;
            BufferedReader r = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = r.readLine()) != null) sb.append(line).append('\n');
            return sb.toString().trim();
        }
    }

    /** Tree-equivalent JSON comparison; tolerates JSONObject.NULL on either side. */
    private static void assertJsonEquals(String msg, Object expected, Object actual) {
        if (expected == JSONObject.NULL || expected == null) {
            assertTrue(msg + " (null) expected null actual=" + actual,
                    actual == null || actual == JSONObject.NULL);
            return;
        }
        if (actual == JSONObject.NULL || actual == null) {
            assertTrue(msg + " (null) actual null expected=" + expected, false);
            return;
        }
        if (expected instanceof JSONObject && actual instanceof JSONObject) {
            JSONObject e = (JSONObject) expected;
            JSONObject a = (JSONObject) actual;
            assertEquals(msg + " (key set)", normalizeKeys(e), normalizeKeys(a));
            for (Iterator<String> it = e.keys(); it.hasNext(); ) {
                String k = it.next();
                Object eVal = e.isNull(k) ? JSONObject.NULL : e.get(k);
                Object aVal = a.isNull(k) ? JSONObject.NULL : a.get(k);
                assertJsonEquals(msg + "." + k, eVal, aVal);
            }
            return;
        }
        if (expected instanceof JSONArray && actual instanceof JSONArray) {
            JSONArray e = (JSONArray) expected;
            JSONArray a = (JSONArray) actual;
            assertEquals(msg + " (array length)", e.length(), a.length());
            for (int i = 0; i < e.length(); i++) {
                assertJsonEquals(msg + "[" + i + "]", e.get(i), a.get(i));
            }
            return;
        }
        if (expected instanceof Number && actual instanceof Number) {
            double ed = ((Number) expected).doubleValue();
            double ad = ((Number) actual).doubleValue();
            assertTrue(msg + " (number) expected=" + ed + " actual=" + ad,
                    Math.abs(ed - ad) < 1e-9);
            return;
        }
        assertEquals(msg, String.valueOf(expected), String.valueOf(actual));
    }

    private static java.util.Set<String> normalizeKeys(JSONObject o) {
        java.util.TreeSet<String> set = new java.util.TreeSet<>();
        for (Iterator<String> it = o.keys(); it.hasNext(); ) set.add(it.next());
        return set;
    }

    /** HttpServletRequest dynamic-proxy returning the body via getReader. */
    private static HttpServletRequest mockRequest(final String body) {
        return (HttpServletRequest) Proxy.newProxyInstance(
                HttpServletRequest.class.getClassLoader(),
                new Class<?>[]{HttpServletRequest.class},
                new InvocationHandler() {
                    @Override
                    public Object invoke(Object p, Method m, Object[] a) throws Throwable {
                        if ("getReader".equals(m.getName())) {
                            return new BufferedReader(new InputStreamReader(
                                    new ByteArrayInputStream(body.getBytes(StandardCharsets.UTF_8)),
                                    StandardCharsets.UTF_8));
                        }
                        if ("getHeaderNames".equals(m.getName())) {
                            return java.util.Collections.enumeration(java.util.Collections.<String>emptyList());
                        }
                        Class<?> ret = m.getReturnType();
                        if (ret == boolean.class) return Boolean.FALSE;
                        if (ret == int.class) return Integer.valueOf(0);
                        if (ret == long.class) return Long.valueOf(0L);
                        if (ret == void.class) return null;
                        if (ret.isPrimitive()) return Integer.valueOf(0);
                        return null;
                    }
                });
    }
}
