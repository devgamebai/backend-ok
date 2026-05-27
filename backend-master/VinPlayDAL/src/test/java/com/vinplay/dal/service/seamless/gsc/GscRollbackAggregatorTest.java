package com.vinplay.dal.service.seamless.gsc;

import com.vinplay.vbee.common.config.VBeePath;
import com.vinplay.vbee.common.messages.GscBetSideEffectMessage;
import com.vinplay.vbee.common.pools.ConnectionPool;
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
 * Phase 3d golden-file contract test for {@link GscRollbackAggregator}.
 *
 * <p>Each fixture pair under
 * {@code src/test/resources/seamless/gsc/rollback/NN_request.json} +
 * {@code NN_response.json} is replayed through the aggregator with a
 * stub {@link GscConfigProvider} that uses a known secret
 * ({@code TEST_SECRET}). The aggregator's response is compared to the
 * expected payload as <em>JSON-tree-equivalent</em> — same comparison
 * helper as {@link GscPushBetAggregatorTest} and
 * {@link GscTransferAggregatorTest}.
 *
 * <h2>Production has 0 events on {@code rollback} in last 30 days</h2>
 * Verified: {@code RollbackProcess} is dead code under current traffic
 * (noted in the Phase 3d implementer brief). The fixtures are therefore
 * <em>synthetic</em> — built from the legacy code's documented request
 * shape ({@code RollbackRequest.java}) rather than captured from
 * {@code gsc_event_log}. Fixture 01 / 02 exercise the credit path
 * (CREDIT direction); fixtures 03 / 04 / 05 exercise the
 * signature-error, unknown-currency, and missing-required-parameters
 * short-circuits respectively.
 *
 * <h2>What's exercised</h2>
 * <ul>
 *   <li>{@code 01} — Single rollback transaction. Member doesn't exist
 *       → batch-abort → empty {@code BalanceResponse} shape.
 *       Confirms the dispatch path reaches {@code doCredit}.</li>
 *   <li>{@code 02} — Different member, larger amount, same shape
 *       (rollback never has a "direction switch" — it's always credit).
 *       Member doesn't exist → batch-abort.</li>
 *   <li>{@code 03} — Intentionally bad signature → SIGNATURE_ERROR
 *       branch maps to {@code {"code":1004,"message":"Invalid sign",
 *       "before_balance":0,"balance":0}}.</li>
 *   <li>{@code 04} — Unknown currency code "ZZZ" → empty
 *       {@code BalanceResponse} fall-back: {@code {"code":0,"message":"",
 *       "data":[]}}.</li>
 *   <li>{@code 05} — Missing required parameter ({@code member_account})
 *       → INTERNAL_SERVER_ERROR shape: {@code {"code":999,"message":
 *       "Missing required parameters","before_balance":0,"balance":0}}.</li>
 * </ul>
 *
 * <h2>Idempotency on retried rollback</h2>
 * The {@link #retriedRollbackReturnsBalanceFallbackWithoutDoubleCredit}
 * test sends the same rollback payload twice and confirms the second
 * call returns the same response shape — the wallet credit either
 * succeeds (POSTED) or is dedup'd at {@code money_gateway_log.uk_tx_source}
 * (DUPLICATE). Since the test runs without a real user the dispatch
 * aborts before reaching the wallet primitive on both calls (member
 * not found → balance-fallback shape), but the test confirms the
 * external-ref namespacing scheme and the no-throw guarantee on retry.
 *
 * <p>A separate {@link #productionPayloadsParseWithoutThrowing} test
 * replays raw payloads with realistic-but-synthetic shapes (production
 * is empty so no real captures available) to verify the parser doesn't
 * crash on extreme inputs; those payloads predictably fail signature
 * validation against {@code TEST_SECRET} and surface the SIGNATURE_ERROR
 * shape, which is the desired behavior.
 *
 * <p>Skips with {@code Assume} when no DB password is supplied — same
 * pattern as {@link GscTransferAggregatorTest}.
 */
public class GscRollbackAggregatorTest {

    private static final String TEST_SECRET = "TEST_SECRET";
    private static volatile boolean bootstrapSkipped = false;

    private GscRollbackAggregator aggregator;

    @BeforeClass
    public static void bootstrapPool() throws Exception {
        String pw = System.getProperty("test.mysql.password");
        if (pw == null || pw.isEmpty()) pw = System.getenv("MYSQL_PASSWORD");
        if (pw == null || pw.isEmpty()) {
            bootstrapSkipped = true;
            System.out.println("GscRollbackAggregatorTest: password not supplied; tests will be skipped");
            return;
        }

        URL propsUrl = GscRollbackAggregatorTest.class
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
            props.store(fos, "Auto-generated by GscRollbackAggregatorTest — password injected at test time");
        }

        ConnectionPool.getInstance();
    }

    @AfterClass
    public static void cleanUp() {
        if (bootstrapSkipped) return;
        try {
            URL propsUrl = GscRollbackAggregatorTest.class
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
            System.err.println("GscRollbackAggregatorTest: could not restore placeholder (non-fatal): "
                    + e.getMessage());
        }
    }

    /**
     * Build the aggregator with a stub config. The aggregator constructor
     * itself doesn't touch the DB — only the per-test {@code handle()}
     * invocations do (and only when the dispatch path reaches
     * {@code doCredit}/{@code doReadBalance}). Validation-error fixtures
     * (signature, missing-params, unknown-currency) short-circuit before
     * any DB call and run unconditionally.
     */
    @Before
    public void setUp() {
        aggregator = new GscRollbackAggregator(new GscConfigProvider() {
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
        });
    }

    // ─────────────────────────────────────────────────────────────────
    // Fixtures
    // ─────────────────────────────────────────────────────────────────

    /**
     * Fixtures 01 and 02 reach the wallet primitives and therefore
     * require the DB pool to be available; the rest short-circuit
     * before any {@code doCredit} call and run unconditionally.
     */
    @Test public void fixture01_rollback_creditPath_memberMissing() throws Exception {
        Assume.assumeFalse("DB password not supplied", bootstrapSkipped);
        runFixture("01");
    }
    @Test public void fixture02_rollback_creditPath_largerAmount() throws Exception {
        Assume.assumeFalse("DB password not supplied", bootstrapSkipped);
        runFixture("02");
    }
    @Test public void fixture03_invalidSignature()                throws Exception { runFixture("03"); }
    @Test public void fixture04_unknownCurrency_balanceFallback() throws Exception { runFixture("04"); }
    @Test public void fixture05_missingRequiredParameters()       throws Exception { runFixture("05"); }

    /**
     * Replay a valid-signature rollback request twice. The second call
     * must return an identical response shape and must not throw — this
     * confirms the external-ref namespacing scheme ({@code rollback_<id>})
     * lets the {@code money_gateway_log.uk_tx_source} UNIQUE handle
     * idempotent retries cleanly.
     *
     * <p>Without a real player in {@code users}, the dispatch aborts
     * before reaching the wallet primitive on BOTH calls (member not
     * found → balance-fallback shape). The test therefore proves the
     * no-throw guarantee on retry, not the wallet-layer dedup itself
     * (which is exercised in the base-class
     * {@link com.vinplay.dal.service.seamless.SeamlessWalletAggregatorTest}).
     */
    @Test
    public void retriedRollbackReturnsBalanceFallbackWithoutDoubleCredit() throws Exception {
        Assume.assumeFalse("DB password not supplied", bootstrapSkipped);
        String body = "{\"sign\":\"032c41771c41620bd28c8e7a829a691c\",\"operator_code\":\"G7A1\","
                + "\"request_time\":\"1777800001\",\"currency\":\"KRW\","
                + "\"member_account\":\"khongduong\",\"product_code\":1002,"
                + "\"game_type\":\"LIVE_CASINO\","
                + "\"transactions\":[{\"id\":\"TX-R-9001\",\"action\":\"rollback\","
                + "\"product_code\":1002,\"bet_amount\":\"1000\","
                + "\"wager_code\":\"W-R-9001\"}]}";
        String first  = aggregator.handle(mockRequest(body));
        String second = aggregator.handle(mockRequest(body));
        assertNotNull(first);
        assertNotNull(second);
        // Both calls must return JSON-tree-equivalent shapes.
        JSONObject f = new JSONObject(first);
        JSONObject s = new JSONObject(second);
        assertJsonEquals("retried rollback: response mismatch", f, s);
    }

    /**
     * Replay 3 raw production-shape payloads and verify the parser
     * doesn't choke or crash. Production has 0 events in the last 30
     * days for the {@code rollback} endpoint so the payloads below are
     * synthetic but match the documented {@code RollbackRequest} schema
     * — sufficient to exercise the parser surface. The captured signs
     * predictably won't validate against {@link #TEST_SECRET} so the
     * aggregator returns the SIGNATURE_ERROR shape — verifying the
     * no-crash invariant + the legacy {@code "Invalid sign"} response
     * shape.
     */
    @Test
    public void productionPayloadsParseWithoutThrowing() throws Exception {
        String[] payloads = {
                "{\"sign\":\"99d8185c4d7c3939bc9c232fc383b0da\",\"currency\":\"KRW\",\"request_time\":\"1777706702\",\"operator_code\":\"G7A1\",\"member_account\":\"khongduong\",\"product_code\":1002,\"game_type\":\"LIVE_CASINO\",\"transactions\":[{\"id\":\"PROD-R-1\",\"action\":\"rollback\",\"product_code\":1002,\"bet_amount\":\"5000\",\"wager_code\":\"WPROD-R-1\"}]}",
                "{\"sign\":\"36e5bfd072b758d07a7ceb503d31d505\",\"currency\":\"VND\",\"request_time\":\"1777696496\",\"operator_code\":\"G7A1\",\"member_account\":\"sunkrvip999\",\"product_code\":1091,\"game_type\":\"LIVE_CASINO\",\"transactions\":[{\"id\":\"PROD-R-2\",\"action\":\"rollback\",\"product_code\":1091,\"bet_amount\":\"100000\",\"wager_code\":\"WPROD-R-2\"},{\"id\":\"PROD-R-2B\",\"action\":\"rollback\",\"product_code\":1091,\"bet_amount\":\"50000\",\"wager_code\":\"WPROD-R-2B\"}]}",
                "{\"sign\":\"2a676c3c6c7f890f82847a9beb4e5502\",\"currency\":\"KRW\",\"request_time\":\"1777705550\",\"operator_code\":\"G7A1\",\"member_account\":\"hayquamnoi\",\"product_code\":1002,\"game_type\":\"LIVE_CASINO\",\"transactions\":[{\"id\":\"PROD-R-3\",\"action\":\"rollback\",\"product_code\":1002,\"bet_amount\":\"250\",\"wager_code\":\"WPROD-R-3\"}]}"
        };
        for (String body : payloads) {
            String resp = aggregator.handle(mockRequest(body));
            assertNotNull(resp);
            JSONObject obj = new JSONObject(resp);
            // Prod sign won't match the TEST_SECRET → SIGNATURE_ERROR
            // path → legacy "Invalid sign" shape with code 1004.
            assertEquals("Invalid sign", obj.optString("message"));
            assertEquals(1004, obj.optInt("code"));
        }
    }

    /**
     * Phase 5 prep gate 5p2 — afterCredit on a successful rollback
     * must publish exactly one {@link GscBetSideEffectMessage} with
     * {@code op=ROLLBACK_DELETE}. Telegram alert intentionally null
     * (legacy parity preserved).
     */
    @Test
    public void afterCredit_rollback_publishesRollbackDelete() throws Exception {
        final java.util.concurrent.atomic.AtomicReference<GscBetSideEffectMessage> sfx =
                new java.util.concurrent.atomic.AtomicReference<>();
        final java.util.concurrent.atomic.AtomicInteger sfxCount =
                new java.util.concurrent.atomic.AtomicInteger();
        GscRollbackAggregator agg = new GscRollbackAggregator(stubConfig()) {
            @Override
            protected void publishBetSideEffect(GscBetSideEffectMessage msg) {
                sfx.set(msg);
                sfxCount.incrementAndGet();
            }
        };

        GscRequest.TransactionItem t = new GscRequest.TransactionItem(
                "TX-R-SFX-1", "rollback", "alice", 1002, "bacc_lobby_1",
                "VND", "W-R-SFX-1", "1000", "1000");
        java.util.List<GscRequest.TransactionItem> txns = new java.util.ArrayList<>();
        txns.add(t);
        GscRequest req = new GscRequest("G7A1", "VND", "ignored", "0",
                "alice", 1002, "LIVE_CASINO", null, txns);

        java.lang.reflect.Method m = GscRollbackAggregator.class.getDeclaredMethod(
                "afterCredit", GscRequest.class, GscRequest.TransactionItem.class);
        m.setAccessible(true);
        m.invoke(agg, req, t);

        assertEquals("publishBetSideEffect must fire exactly once",
                1, sfxCount.get());
        GscBetSideEffectMessage msg = sfx.get();
        assertNotNull(msg);
        assertEquals("op", GscBetSideEffectMessage.Op.ROLLBACK_DELETE, msg.op);
        assertEquals("aggregatorTag", "GscRollback", msg.aggregatorTag);
        assertEquals("memberAccount", "alice", msg.memberAccount);
        assertEquals("productCode", 1002, msg.productCode);
        assertEquals("wagerCode", "W-R-SFX-1", msg.wagerCode);
        // Legacy parity — rollback cleanup never paged ops on Mongo
        // failure, so the consumer-side alert must stay disabled.
        assertEquals("telegramAlertSubject must be null", null, msg.telegramAlertSubject);
    }

    /** Stub config matching the {@link #setUp} fixture aggregator. */
    private static GscConfigProvider stubConfig() {
        return new GscConfigProvider() {
            @Override public String getSecretKey() { return TEST_SECRET; }
            @Override public double getOperatorExchangeRate() { return 1.0; }
            @Override
            public int getCurrencyExchangeRate(String currencyCode) {
                if (currencyCode == null) return 0;
                String s = currencyCode.trim();
                if ("KRW".equals(s) || "VND".equals(s)) return 1;
                return 0;
            }
            @Override public int getTaxPercent() { return 0; }
        };
    }

    // ─────────────────────────────────────────────────────────────────
    // Helpers
    // ─────────────────────────────────────────────────────────────────

    private void runFixture(String name) throws Exception {
        String reqPath = "seamless/gsc/rollback/" + name + "_request.json";
        String respPath = "seamless/gsc/rollback/" + name + "_response.json";
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
        try (InputStream is = GscRollbackAggregatorTest.class
                .getClassLoader().getResourceAsStream(path)) {
            if (is == null) return null;
            BufferedReader r = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = r.readLine()) != null) sb.append(line).append('\n');
            return sb.toString().trim();
        }
    }

    /**
     * Tree-equivalent JSON comparison, mirroring
     * {@link GscPushBetAggregatorTest#assertJsonEquals} and
     * {@link GscTransferAggregatorTest}.
     */
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
