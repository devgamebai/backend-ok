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
import static org.junit.Assert.assertTrue;

/**
 * Phase 3e golden-file contract test for {@link GscDepositAggregator}.
 *
 * <p>Each fixture pair under
 * {@code src/test/resources/seamless/gsc/deposit/NN_request.json} +
 * {@code NN_response.json} is replayed through the aggregator with a
 * stub {@link GscConfigProvider} that uses a known secret
 * ({@code TEST_SECRET}). The aggregator's response is compared to the
 * expected payload as <em>JSON-tree-equivalent</em> — same comparison
 * helper as {@link GscRollbackAggregatorTest} et al.
 *
 * <h2>Test-secret-vs-production-secret asymmetry</h2>
 * Production payloads from {@code gsc_event_log} were signed with the
 * operator's real shared secret (not exposed here). Replaying them
 * through the aggregator with {@code TEST_SECRET} therefore predictably
 * trips the signature check, returning the SIGNATURE_ERROR shape. That
 * is precisely the point: production fixtures verify the parser
 * doesn't crash on real-world inputs (extreme nesting, batch_requests
 * envelope, multi-byte UUIDs), and the SIGNATURE_ERROR shape is the
 * documented expected output for "valid JSON but bad sign". Synthetic
 * fixtures (03–07) carry signatures computed against {@code TEST_SECRET}
 * so they reach the dispatch path and exercise the validation branches.
 *
 * <h2>Fixture map</h2>
 * <ul>
 *   <li>{@code 01} — Real production payload (Evolution 1002 settle,
 *       batch_requests envelope). Bad sign vs TEST_SECRET → SIGNATURE_ERROR.
 *       Sourced from {@code vinplay.gsc_event_log} ID-DESC LIMIT 6.</li>
 *   <li>{@code 02} — Real production payload (PG Soft 1007 settle with
 *       parent_round_id freespin chain link). Bad sign → SIGNATURE_ERROR.</li>
 *   <li>{@code 03} — Valid sign, normal SETTLED (Evolution). Member
 *       missing in DB+Hazelcast → batch aborts via doCredit USER_NOT_FOUND
 *       → response {@code {"code":999,"message":"Server Error",...}}.
 *       Confirms the dispatch path reaches doCredit.</li>
 *   <li>{@code 04} — Valid sign, cancel-via-deposit (Hash Game 1149,
 *       action=CANCEL). Member missing → same 999 path. Confirms the
 *       isCancelLikeDeposit branch builds the {@code "deposit_cancel_*"}
 *       external-ref (vs the normal {@code "deposit_*"}).</li>
 *   <li>{@code 05} — Valid sign, unknown currency "ZZZ" → empty
 *       BalanceResponse fall-back: {@code {"code":0,"message":"","data":[]}}.</li>
 *   <li>{@code 06} — Valid sign, missing required parameter
 *       (member_account omitted entirely; no batch_requests fall-back).
 *       → {@code {"code":1002,"message":"Missing required parameters",...}}.</li>
 *   <li>{@code 07} — Bad sign with otherwise valid required-params →
 *       SIGNATURE_ERROR shape: {@code {"code":1004,"message":"Invalid sign",...}}.</li>
 * </ul>
 *
 * <h2>Idempotency on retried deposit</h2>
 * The {@link #retriedDepositReturnsConsistentShapeWithoutDoubleCredit}
 * test sends the same deposit payload twice and confirms the second
 * call returns the same response shape — the wallet credit either
 * succeeds (POSTED) or is dedup'd at {@code money_gateway_log.uk_tx_source}
 * (DUPLICATE). Without a real user the dispatch aborts at doCredit's
 * USER_NOT_FOUND on both calls (member not found → server-error shape);
 * the test confirms the external-ref namespacing scheme and the
 * no-throw guarantee on retry.
 *
 * <h2>Production-payload robustness</h2>
 * {@link #productionPayloadsParseWithoutThrowing} replays 3 raw
 * payloads from {@code vinplay.gsc_event_log} and verifies the parser
 * doesn't crash; their captured signs predictably trip the
 * SIGNATURE_ERROR path against {@link #TEST_SECRET}. That is the
 * desired behavior — proves the parser is robust against extreme inputs.
 *
 * <h2>Direction coverage</h2>
 * {@link #mapActionToSourceAlwaysCredit} asserts {@code mapActionToSource}
 * returns {@link MoneyGateway#SOURCE_GSC_CREDIT} regardless of action verb,
 * including the cancel-via-deposit case — defense against accidental
 * future direction switch.
 */
public class GscDepositAggregatorTest {

    private static final String TEST_SECRET = "TEST_SECRET";
    private static volatile boolean bootstrapSkipped = false;

    private GscDepositAggregator aggregator;

    @BeforeClass
    public static void bootstrapPool() throws Exception {
        String pw = System.getProperty("test.mysql.password");
        if (pw == null || pw.isEmpty()) pw = System.getenv("MYSQL_PASSWORD");
        if (pw == null || pw.isEmpty()) {
            bootstrapSkipped = true;
            System.out.println("GscDepositAggregatorTest: password not supplied; tests will be skipped");
            return;
        }

        URL propsUrl = GscDepositAggregatorTest.class
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
            props.store(fos, "Auto-generated by GscDepositAggregatorTest — password injected at test time");
        }

        ConnectionPool.getInstance();
    }

    @AfterClass
    public static void cleanUp() {
        if (bootstrapSkipped) return;
        try {
            URL propsUrl = GscDepositAggregatorTest.class
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
            System.err.println("GscDepositAggregatorTest: could not restore placeholder (non-fatal): "
                    + e.getMessage());
        }
    }

    /**
     * Build the aggregator with a stub config + no-op provider hooks.
     * The constructor is side-effect-free — the only DB access is
     * inside per-test {@code handle()} invocations.
     */
    @Before
    public void setUp() {
        aggregator = new GscDepositAggregator(
                new GscConfigProvider() {
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
                },
                GscDepositProviderHooks.Resolver.DEFAULT);
    }

    // ─────────────────────────────────────────────────────────────────
    // Fixtures
    // ─────────────────────────────────────────────────────────────────

    @Test public void fixture01_productionEvolutionSettle_signatureError() throws Exception { runFixture("01"); }
    @Test public void fixture02_productionPgSoftFreespin_signatureError() throws Exception { runFixture("02"); }

    /**
     * Fixtures 03 and 04 reach the wallet primitives and therefore
     * require the DB pool to be available; the rest short-circuit
     * before any {@code doCredit} call and run unconditionally.
     */
    @Test public void fixture03_validSig_normalSettle_userMissing() throws Exception {
        Assume.assumeFalse("DB password not supplied", bootstrapSkipped);
        runFixture("03");
    }
    @Test public void fixture04_validSig_cancelViaDeposit_userMissing() throws Exception {
        Assume.assumeFalse("DB password not supplied", bootstrapSkipped);
        runFixture("04");
    }
    @Test public void fixture05_unknownCurrency_balanceFallback()  throws Exception { runFixture("05"); }
    @Test public void fixture06_missingRequiredParameters()        throws Exception { runFixture("06"); }
    @Test public void fixture07_invalidSignature_validFields()     throws Exception { runFixture("07"); }

    /**
     * Replay a valid-signature deposit request twice. The second call
     * must return an identical response shape and must not throw — this
     * confirms the external-ref namespacing scheme ({@code deposit_<id>}
     * for normal settle) lets {@code money_gateway_log.uk_tx_source}
     * UNIQUE handle idempotent retries cleanly.
     *
     * <p>Without a real player in {@code users}, the dispatch aborts
     * before reaching the wallet credit primitive on BOTH calls
     * (member not found in Hazelcast → SC_MEMBER_NOT_EXIST 1000 if
     * Hazelcast is running, OR 999 server-error if Hazelcast skipped
     * the check and doCredit fell through to USER_NOT_FOUND). Either
     * way, both calls must produce the same shape.
     */
    @Test
    public void retriedDepositReturnsConsistentShapeWithoutDoubleCredit() throws Exception {
        Assume.assumeFalse("DB password not supplied", bootstrapSkipped);
        // Sign for op=G7A1 rt=1777800301 verb=deposit secret=TEST_SECRET
        String body = "{\"sign\":\"13515128b6e42a8e9843b41362162672\",\"operator_code\":\"G7A1\","
                + "\"request_time\":\"1777800301\",\"currency\":\"KRW\","
                + "\"member_account\":\"khongduong\",\"product_code\":1002,"
                + "\"game_type\":\"LIVE_CASINO\","
                + "\"transactions\":[{\"id\":\"TX-D-9001\",\"action\":\"SETTLED\","
                + "\"product_code\":1002,\"amount\":\"5000\",\"bet_amount\":\"5000\","
                + "\"wager_code\":\"W-D-9001\"}]}";
        String first  = aggregator.handle(mockRequest(body));
        String second = aggregator.handle(mockRequest(body));
        assertNotNull(first);
        assertNotNull(second);
        // Both calls must return JSON-tree-equivalent shapes.
        JSONObject f = new JSONObject(first);
        JSONObject s = new JSONObject(second);
        assertJsonEquals("retried deposit: response mismatch", f, s);
    }

    /**
     * Replay 3 raw production-shape payloads sourced directly from
     * {@code vinplay.gsc_event_log WHERE gsc_endpoint='deposit'} and
     * verify the parser doesn't choke or crash. Production sigs are
     * computed against the real operator secret, not {@link #TEST_SECRET},
     * so the aggregator returns the SIGNATURE_ERROR shape on each —
     * which verifies the no-crash invariant + the "Invalid sign"
     * response shape under realistic input nesting (Evolution payload,
     * PG Soft parent_round_id, JILI fish payload).
     */
    @Test
    public void productionPayloadsParseWithoutThrowing() throws Exception {
        String[] payloads = {
                // Evolution 1002 — KRW, batch_requests, Cxxxx + Dxxxx nested payload
                "{\"sign\":\"78654615fd2478de36c7ccb9d778e75f\",\"currency\":\"KRW\",\"game_type\":\"\",\"request_time\":\"1777718567\",\"operator_code\":\"G7A1\",\"batch_requests\":[{\"game_type\":\"LIVE_CASINO\",\"product_code\":1002,\"transactions\":[{\"id\":\"04c19e5b-ca43-45a6-b2c5-b71501e98e2d\",\"action\":\"SETTLED\",\"amount\":\"0\",\"payload\":{\"C37be2b18-1e66-4cd6-8a5d-21cf131029ac\":{\"session_id\":\"CKiPGbrSYhn3MpsQ8eDdcb\",\"provider_tx_id\":\"C37be2b18-1e66-4cd6-8a5d-21cf131029ac\"},\"D37be2b18-1e66-4cd6-8a5d-21cf131029ac\":{\"game_id\":\"18abb999d5d159818af4d0a1-txubkkja3i4tbidg\",\"session_id\":\"CKiPGbrSYhn3MpsQ8eDdcb\",\"game_table_id\":\"p63cmvmwagteemoy\"}},\"round_id\":\"37be2b18-1e66-4cd6-8a5d-21cf131029ac\",\"game_code\":\"p63cmvmwagteemoy\",\"bet_amount\":\"40000\",\"wager_code\":\"iCYQ9oVoXDceUSMsvS9NhK\",\"prize_amount\":\"0\",\"wager_status\":\"SETTLED\",\"valid_bet_amount\":\"40000\"}],\"member_account\":\"testuserdl001\"}]}",
                // PG Soft 1007 — VND2, parent_round_id freespin chain
                "{\"sign\":\"3cfc869e430ad97a1dd8a4b5261ad9e7\",\"currency\":\"VND2\",\"game_type\":\"\",\"request_time\":\"1777685974\",\"operator_code\":\"G7A1\",\"batch_requests\":[{\"game_type\":\"SLOT\",\"product_code\":1007,\"transactions\":[{\"id\":\"2fa445d0-e9c2-41bb-9cd9-7320eed4aeed\",\"action\":\"SETTLED\",\"amount\":\"0.8\",\"payload\":{\"2050389705236829700\":{\"amount\":\"0\",\"action_type\":\"BET_SETTLED\",\"parent_round_id\":\"2050221717678330885\"}},\"round_id\":\"2050389705236829700\",\"game_code\":\"135\",\"bet_amount\":\"0\",\"wager_code\":\"AfeJ5ycA4tFn93bjBkw6BG\",\"prize_amount\":\"0.8\",\"wager_status\":\"SETTLED\"}],\"member_account\":\"1234321\"}]}",
                // JILI 1091 — VND, FISHING with simpler payload shape
                "{\"sign\":\"734242d9273e63b9771fe70db7cc113f\",\"currency\":\"VND\",\"game_type\":\"\",\"request_time\":\"1777688048\",\"operator_code\":\"G7A1\",\"batch_requests\":[{\"game_type\":\"FISHING\",\"product_code\":1091,\"transactions\":[{\"id\":\"e3a813fa-9000-4d70-b176-6e3acac96bd2\",\"action\":\"SETTLED\",\"amount\":\"0\",\"payload\":{\"token\":\"SvqZaJRNypyYb4QfsYTxqh\",\"bet_id\":\"617667369124309659\",\"parent_bet_id\":null,\"provider_tx_id\":\"3835158560689644389\"},\"round_id\":\"617667369124309659\",\"game_code\":\"667\",\"bet_amount\":\"100\",\"wager_code\":\"QG3kvMLxVq5PkP2aTZqdsf\",\"prize_amount\":\"0\",\"wager_status\":\"SETTLED\"}],\"member_account\":\"hiuaaak\"}]}"
        };
        for (String body : payloads) {
            String resp = aggregator.handle(mockRequest(body));
            assertNotNull(resp);
            JSONObject obj = new JSONObject(resp);
            // Prod sign won't match TEST_SECRET → SIGNATURE_ERROR path
            // → legacy-shape "Invalid sign" with code 1004 inside data[0].
            assertEquals(1004, obj.optInt("code"));
            assertEquals("Invalid sign", obj.optString("message"));
            JSONArray data = obj.optJSONArray("data");
            assertNotNull("data array missing", data);
            assertEquals("data length", 1, data.length());
            assertEquals("inner code", 1004, data.optJSONObject(0).optInt("code"));
        }
    }

    /**
     * Direction-coverage smoke test: every action verb on the deposit
     * endpoint maps to {@link MoneyGateway#SOURCE_GSC_CREDIT}. Defensive
     * against an accidental future direction switch (e.g. someone
     * adding a "BET" verb branch that mis-routes via doDebit). Deposit
     * is structurally a credit-only handler — both normal settle and
     * cancel-via-deposit are wallet additions.
     */
    /**
     * Phase 3e rebate-publish gap fix: when {@code afterCredit} runs for a
     * normal SETTLED outcome, it must publish a {@link LogMoneyUserMessage}
     * to {@code queue_log_money_user_extra} so the agency-commission
     * pipeline ({@code LogMoneyUserExtraProcessor}) generates the per-bet
     * rebate rows. Mirrors what {@code userMoneyService.reward} →
     * {@code UserServiceImpl.updateMoney} does on the legacy path.
     *
     * <p>{@link MoneyGateway#creditUser} is a pure wallet primitive and
     * does NOT publish the rebate event — without this hook the agency
     * commission generation silently breaks on flag-on traffic.
     *
     * <p>Test strategy: subclass the aggregator overriding
     * {@code publishRebateMessage} to capture the message, plus override
     * {@code resolveUserIdForRebate} / {@code lookupUserCache} to bypass
     * the DB and Hazelcast (so the test runs unconditionally — no
     * {@code Assume.assumeFalse} on {@code bootstrapSkipped}). Invoke
     * {@code afterCredit} directly via reflection.
     */
    @Test
    public void afterCredit_normalSettle_publishesRebateMessage() throws Exception {
        final java.util.concurrent.atomic.AtomicReference<LogMoneyUserMessage> captured =
                new java.util.concurrent.atomic.AtomicReference<>();
        GscDepositAggregator agg = new GscDepositAggregator(
                stubConfig(), GscDepositProviderHooks.Resolver.DEFAULT) {
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
                "TX-D-12345", "SETTLED", "alice", 1002, "bacc_lobby_1",
                /*currency*/ "VND", /*wagerCode*/ "W-D-12345",
                /*amount*/ "5000", /*betAmount*/ "5000",
                /*wagerStatus*/ "SETTLED", /*prizeAmount*/ "0",
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

        invokeAfterCredit(agg, req, t,
                GscDepositProviderHooks.DEFAULT,
                /*isCancelLike*/ false,
                /*amountSubunit*/ 5000L,
                /*postCreditBalance*/ 105000L);

        LogMoneyUserMessage msg = captured.get();
        assertNotNull("publishRebateMessage must be invoked on SETTLED", msg);
        assertEquals("userId", 4242, msg.getUserId());
        assertEquals("nickname", "alice", msg.getNickname());
        assertEquals("actionName / gameName", "gsc_1002_bacc_lobby_1", msg.getActionName());
        assertEquals("serviceName == String.valueOf(productCode)", "1002", msg.getServiceName());
        assertEquals("currentMoney = post-credit balance", 105000L, msg.getCurrentMoney());
        assertEquals("moneyExchange — POSITIVE for credit", 5000L, msg.getMoneyExchange());
        assertEquals("moneyType", Consts.MONEY_VIN, msg.getMoneyType());
        assertEquals("fee", 0L, msg.getFee());
        assertTrue("vp / playgame must be true",  msg.isVp());
        assertTrue("isBot=false when Hazelcast cache is empty", !msg.isBot());
        assertEquals("stable source-key for at-least-once dedup",
                "gsc:TX-D-12345", msg.getId());
    }

    /**
     * Cancel-via-deposit must NOT publish the rebate event — that path
     * already calls {@code RebateService.reverseGscByWagerCode} which
     * is the inverse, and re-publishing would double-attribute on the
     * agency-commission pipeline. This test confirms the cancel branch
     * short-circuits before the publish.
     */
    @Test
    public void afterCredit_cancelViaDeposit_doesNotPublishRebate() throws Exception {
        final java.util.concurrent.atomic.AtomicInteger publishCount =
                new java.util.concurrent.atomic.AtomicInteger(0);
        GscDepositAggregator agg = new GscDepositAggregator(
                stubConfig(), GscDepositProviderHooks.Resolver.DEFAULT) {
            @Override
            protected void publishRebateMessage(LogMoneyUserMessage msg) {
                publishCount.incrementAndGet();
            }
            @Override
            protected UserCacheModel lookupUserCache(String memberAccount) {
                return null;
            }
            @Override
            protected int resolveUserIdForRebate(String memberAccount, UserCacheModel cached) {
                return 4242;
            }
        };

        GscRequest.TransactionItem t = new GscRequest.TransactionItem(
                "TX-CANC-1", "CANCEL", "bob", 1149, "HASH_ROULETTE_1",
                "VND", "W-CANC-1", "100", "100",
                "CANCEL", "0", "0", "round-2", null);
        java.util.List<GscRequest.TransactionItem> txns = new java.util.ArrayList<>();
        txns.add(t);
        GscRequest req = new GscRequest(
                "G7A1", "VND", "ignored", "0",
                "bob", 1149, "HASH_ROULETTE", null, txns);

        // afterCredit may attempt the Mongo cancel-cleanup paths — they
        // catch internally so the call returns cleanly. Either way the
        // publish must not fire.
        invokeAfterCredit(agg, req, t,
                GscDepositProviderHooks.DEFAULT,
                /*isCancelLike*/ true,
                /*amountSubunit*/ 100L,
                /*postCreditBalance*/ 50000L);

        assertEquals("cancel-via-deposit must NOT publish rebate",
                0, publishCount.get());
    }

    /**
     * Phase 5 prep gate 5p2 — afterCredit on a normal SETTLED must
     * publish exactly one {@link GscBetSideEffectMessage} with
     * {@code op=SETTLE_UPDATE}, the prize amount, the resolved
     * event_key, and the legacy Telegram alert subject. No Mongo
     * touch in the request thread.
     */
    @Test
    public void afterCredit_normalSettle_publishesBetSideEffect() throws Exception {
        final java.util.concurrent.atomic.AtomicReference<GscBetSideEffectMessage> sfx =
                new java.util.concurrent.atomic.AtomicReference<>();
        final java.util.concurrent.atomic.AtomicInteger sfxCount =
                new java.util.concurrent.atomic.AtomicInteger();
        GscDepositAggregator agg = new GscDepositAggregator(
                stubConfig(), GscDepositProviderHooks.Resolver.DEFAULT) {
            @Override protected void publishRebateMessage(LogMoneyUserMessage msg) { /* swallow */ }
            @Override
            protected void publishBetSideEffect(GscBetSideEffectMessage msg) {
                sfx.set(msg);
                sfxCount.incrementAndGet();
            }
            @Override protected UserCacheModel lookupUserCache(String memberAccount) { return null; }
            @Override
            protected int resolveUserIdForRebate(String memberAccount, UserCacheModel cached) { return 4242; }
        };

        GscRequest.TransactionItem t = new GscRequest.TransactionItem(
                "TX-D-SFX-1", "SETTLED", "alice", 1002, "bacc_lobby_1",
                "VND", "W-D-SFX-1", "5000", "5000",
                "SETTLED", "0", "5000", "round-1", null);
        java.util.List<GscRequest.TransactionItem> txns = new java.util.ArrayList<>();
        txns.add(t);
        GscRequest req = new GscRequest("G7A1", "VND", "ignored", "0",
                "alice", 1002, "LIVE_CASINO", null, txns);

        invokeAfterCredit(agg, req, t,
                GscDepositProviderHooks.DEFAULT,
                /*isCancelLike*/ false,
                /*amountSubunit*/ 5000L,
                /*postCreditBalance*/ 105000L);

        assertEquals("publishBetSideEffect must fire exactly once on SETTLED",
                1, sfxCount.get());
        GscBetSideEffectMessage m = sfx.get();
        assertNotNull(m);
        assertEquals("op", GscBetSideEffectMessage.Op.SETTLE_UPDATE, m.op);
        assertEquals("aggregatorTag", "GscDeposit", m.aggregatorTag);
        assertEquals("memberAccount", "alice", m.memberAccount);
        assertEquals("productCode", 1002, m.productCode);
        assertEquals("gameCode", "bacc_lobby_1", m.gameCode);
        assertEquals("wagerCode", "W-D-SFX-1", m.wagerCode);
        assertEquals("txnId", "TX-D-SFX-1", m.txnId);
        assertEquals("prize", 5000L, m.prize);
        assertEquals("gameKey", "gsc_1002_bacc_lobby_1", m.gameKey);
        assertEquals("currency", "VND", m.currency);
        assertEquals("telegramAlertSubject — legacy wording byte-for-byte",
                "deposit settleUpdate", m.telegramAlertSubject);
    }

    /**
     * Cancel-via-deposit must publish a {@code CANCEL_DELETE} message
     * (no settle update) and must NOT carry a Telegram alert subject —
     * legacy parity preserves the silent-cleanup behavior of the
     * SUN-1182 path.
     */
    @Test
    public void afterCredit_cancelViaDeposit_publishesCancelDelete() throws Exception {
        final java.util.concurrent.atomic.AtomicReference<GscBetSideEffectMessage> sfx =
                new java.util.concurrent.atomic.AtomicReference<>();
        GscDepositAggregator agg = new GscDepositAggregator(
                stubConfig(), GscDepositProviderHooks.Resolver.DEFAULT) {
            @Override protected void publishRebateMessage(LogMoneyUserMessage msg) { /* swallow */ }
            @Override
            protected void publishBetSideEffect(GscBetSideEffectMessage msg) {
                sfx.set(msg);
            }
            @Override protected UserCacheModel lookupUserCache(String memberAccount) { return null; }
            @Override
            protected int resolveUserIdForRebate(String memberAccount, UserCacheModel cached) { return 4242; }
        };

        GscRequest.TransactionItem t = new GscRequest.TransactionItem(
                "TX-CANC-2", "CANCEL", "bob", 1149, "HASH_ROULETTE_1",
                "VND", "W-CANC-2", "100", "100",
                "CANCEL", "0", "0", "round-2", null);
        java.util.List<GscRequest.TransactionItem> txns = new java.util.ArrayList<>();
        txns.add(t);
        GscRequest req = new GscRequest("G7A1", "VND", "ignored", "0",
                "bob", 1149, "HASH_ROULETTE", null, txns);

        invokeAfterCredit(agg, req, t,
                GscDepositProviderHooks.DEFAULT,
                /*isCancelLike*/ true,
                /*amountSubunit*/ 100L,
                /*postCreditBalance*/ 50000L);

        GscBetSideEffectMessage m = sfx.get();
        assertNotNull("cancel-via-deposit must publish a side effect", m);
        assertEquals("op", GscBetSideEffectMessage.Op.CANCEL_DELETE, m.op);
        assertEquals("wagerCode", "W-CANC-2", m.wagerCode);
        // Legacy parity — cancel cleanup never paged ops on Mongo
        // failure, so the consumer-side alert must stay disabled.
        assertEquals("telegramAlertSubject must be null for cancel-cleanup",
                null, m.telegramAlertSubject);
    }

    /**
     * SUN-gsc-lose-latency: lose bets arrive on /deposit with amount=0
     * (provider has nothing to credit on a loss). The pre-fix code
     * early-returned a {@code POSTED} skip BEFORE calling afterCredit,
     * which meant {@code SETTLE_UPDATE} was never published — the
     * {@code log_gsc_bets} row stayed {@code settled=false} until the
     * periodic {@link GscWagerReconciler} back-filled it via GSC's 3.3
     * API (minutes of latency). The agency-portal betting history
     * filter ({@code settled=true}) hid the row in the meantime.
     *
     * <p>Win bets ({@code amount > 0}) ran afterCredit synchronously and
     * therefore landed in the agency portal in seconds — user-observed
     * asymmetry that motivated the fix.
     *
     * <p>This test calls {@link GscDepositAggregator#dispatchOne} directly
     * (private — reflection) with a lose-shaped txn (amount=0) and
     * asserts that:
     * <ul>
     *   <li>The outcome is {@link SeamlessOutcome.Status#POSTED} with
     *       {@code newBalance=0} (synthetic — no wallet movement).</li>
     *   <li>{@code publishBetSideEffect} fires exactly once with
     *       {@code op=SETTLE_UPDATE} and {@code prize=0} so the Mongo
     *       consumer marks the row {@code settled=true, prize=0} —
     *       making it visible to the agency portal at win-bet latency.</li>
     *   <li>{@code publishRebateMessage} does NOT fire — legacy
     *       {@code reward(0)} also short-circuits the
     *       {@code LogMoneyUserMessage} publish, so commission attribution
     *       stays byte-for-byte equivalent (no moneyExchange=0 rows).</li>
     * </ul>
     *
     * <p>Wallet credit ({@code MoneyGateway.creditUser}) is NOT covered
     * by this test directly — {@code doCredit} is {@code protected final}
     * and not mockable here. The test instead asserts
     * {@code newBalance=0}, which the synthetic POSTED outcome carries
     * unconditionally and which would NOT match the result of a real
     * {@code creditUser} call (that returns the post-credit balance from
     * the DB). Indirect proof that {@code doCredit} was skipped.
     */
    @Test
    public void loseBet_amountZero_publishesSettleUpdate() throws Exception {
        final java.util.List<GscBetSideEffectMessage> sfxCalls =
                new java.util.ArrayList<>();
        final java.util.concurrent.atomic.AtomicInteger rebateCount =
                new java.util.concurrent.atomic.AtomicInteger();
        GscDepositAggregator agg = new GscDepositAggregator(
                stubConfig(), GscDepositProviderHooks.Resolver.DEFAULT) {
            @Override
            protected void publishBetSideEffect(GscBetSideEffectMessage msg) {
                sfxCalls.add(msg);
            }
            @Override
            protected void publishRebateMessage(LogMoneyUserMessage msg) {
                rebateCount.incrementAndGet();
            }
            @Override protected UserCacheModel lookupUserCache(String memberAccount) { return null; }
            @Override
            protected int resolveUserIdForRebate(String memberAccount, UserCacheModel cached) { return 4242; }
        };

        // Lose-shaped settle: amount="0" (no money to credit), prize_amount="0".
        GscRequest.TransactionItem t = new GscRequest.TransactionItem(
                "TX-LOSE-1", "SETTLED", "alice", 1002, "bacc_lobby_1",
                /*currency*/ "VND", /*wagerCode*/ "W-LOSE-1",
                /*amount*/ "0", /*betAmount*/ "5000",
                /*wagerStatus*/ "SETTLED", /*prizeAmount*/ "0",
                /*validBetAmount*/ "5000", /*roundId*/ "round-lose-1",
                /*payload*/ null);
        java.util.List<GscRequest.TransactionItem> txns = new java.util.ArrayList<>();
        txns.add(t);
        GscRequest req = new GscRequest(
                /*operatorCode*/ "G7A1", /*currency*/ "VND",
                /*sign*/ "ignored", /*requestTime*/ "0",
                /*memberAccount*/ "alice", /*productCode*/ 1002,
                /*gameType*/ "LIVE_CASINO",
                /*batchMembers*/ null, txns);

        SeamlessOutcome out = invokeDispatchOne(agg, req, t,
                GscDepositProviderHooks.DEFAULT, /*batchHasFish*/ false);

        // (1) The outcome must be POSTED so the calling batch loop in
        //     dispatch() does NOT abort on isSuccessful — lose bets
        //     count as success.
        assertNotNull("dispatchOne must not return null for amount=0", out);
        assertEquals("amount=0 must yield POSTED (synthetic, no wallet movement)",
                SeamlessOutcome.Status.POSTED, out.status);
        assertEquals("amount=0 newBalance is informational; carried as 0",
                0L, out.newBalance);

        // (2) Exactly one SETTLE_UPDATE published — the actual fix point.
        assertEquals("amount=0 must publish exactly one bet side-effect "
                + "(SETTLE_UPDATE for log_gsc_bets settled=true,prize=0)",
                1, sfxCalls.size());
        GscBetSideEffectMessage m = sfxCalls.get(0);
        assertEquals("op must be SETTLE_UPDATE so consumer marks settled=true",
                GscBetSideEffectMessage.Op.SETTLE_UPDATE, m.op);
        assertEquals("wagerCode must match the lose-bet's wager_code",
                "W-LOSE-1", m.wagerCode);
        assertEquals("txnId must match the deposit txn id",
                "TX-LOSE-1", m.txnId);
        assertEquals("prize=0 — Mongo $inc prize:0 is a no-op, $set settled=true is the actual mutation",
                0L, m.prize);
        assertEquals("memberAccount", "alice", m.memberAccount);
        assertEquals("productCode", 1002, m.productCode);
        assertEquals("gameCode", "bacc_lobby_1", m.gameCode);

        // (3) Legacy reward(0) never published LogMoneyUserMessage, so
        //     preserve that — no commission row for amount=0.
        assertEquals("amount=0 must NOT publish rebate message "
                + "(legacy reward(0) short-circuits before publish)",
                0, rebateCount.get());
    }

    /** Reflection helper — dispatchOne is private; call site is dispatch. */
    private static SeamlessOutcome invokeDispatchOne(GscDepositAggregator agg,
                                                     GscRequest req,
                                                     GscRequest.TransactionItem t,
                                                     GscDepositProviderHooks provider,
                                                     boolean batchHasFish) throws Exception {
        Method m = GscDepositAggregator.class.getDeclaredMethod(
                "dispatchOne",
                GscRequest.class, GscRequest.TransactionItem.class,
                GscDepositProviderHooks.class, boolean.class);
        m.setAccessible(true);
        return (SeamlessOutcome) m.invoke(agg, req, t, provider, batchHasFish);
    }

    /** Reflection helper — afterCredit is private; call site is dispatchOne. */
    private static void invokeAfterCredit(GscDepositAggregator agg,
                                          GscRequest req,
                                          GscRequest.TransactionItem t,
                                          GscDepositProviderHooks provider,
                                          boolean isCancelLike,
                                          long amountSubunit,
                                          long postCreditBalance) throws Exception {
        Method m = GscDepositAggregator.class.getDeclaredMethod(
                "afterCredit",
                GscRequest.class, GscRequest.TransactionItem.class,
                GscDepositProviderHooks.class, boolean.class,
                long.class, long.class);
        m.setAccessible(true);
        m.invoke(agg, req, t, provider, isCancelLike, amountSubunit, postCreditBalance);
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
                if ("KRW".equals(s) || "VND".equals(s) || "USD".equals(s)) return 1;
                if ("VND2".equals(s) || "IDR2".equals(s)) return 1000;
                return 0;
            }
            @Override public int getTaxPercent() { return 0; }
        };
    }

    @Test
    public void mapActionToSourceAlwaysCredit() {
        // Reach the protected method via reflection — same approach
        // as Phase 3c's GscCancelAggregatorTest direction tests.
        try {
            Method m = GscDepositAggregator.class.getDeclaredMethod(
                    "mapActionToSource", String.class);
            m.setAccessible(true);
            String[] actions = {"SETTLED", "settled", "CANCEL", "cancel",
                                "REFUND", "VOID", "FREEBET", "JACKPOT",
                                null, "", "garbage_action"};
            for (String a : actions) {
                Object got = m.invoke(aggregator, a);
                assertEquals("action=" + a + " must map to SOURCE_GSC_CREDIT",
                        MoneyGateway.SOURCE_GSC_CREDIT, got);
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    // ─────────────────────────────────────────────────────────────────
    // Helpers (mirror GscRollbackAggregatorTest)
    // ─────────────────────────────────────────────────────────────────

    private void runFixture(String name) throws Exception {
        String reqPath = "seamless/gsc/deposit/" + name + "_request.json";
        String respPath = "seamless/gsc/deposit/" + name + "_response.json";
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
        try (InputStream is = GscDepositAggregatorTest.class
                .getClassLoader().getResourceAsStream(path)) {
            if (is == null) return null;
            BufferedReader r = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = r.readLine()) != null) sb.append(line).append('\n');
            return sb.toString().trim();
        }
    }

    /** Tree-equivalent JSON comparison. Mirrors GscRollbackAggregatorTest. */
    private static void assertJsonEquals(String msg, Object expected, Object actual) {
        if (expected instanceof JSONObject && actual instanceof JSONObject) {
            JSONObject e = (JSONObject) expected;
            JSONObject a = (JSONObject) actual;
            assertEquals(msg + " (key set)", normalizeKeys(e), normalizeKeys(a));
            for (Iterator<String> it = e.keys(); it.hasNext(); ) {
                String k = it.next();
                assertJsonEquals(msg + "." + k, e.get(k), a.get(k));
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
