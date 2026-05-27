package com.vinplay.dal.service.seamless.gsc;

import com.vinplay.vbee.common.config.VBeePath;
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
 * Phase 3b golden-file contract test for {@link GscTransferAggregator}.
 *
 * <p>Each fixture pair under
 * {@code src/test/resources/seamless/gsc/transfer/NN_request.json} +
 * {@code NN_response.json} is replayed through the aggregator with a
 * stub {@link GscConfigProvider} that uses a known secret
 * ({@code TEST_SECRET}). The aggregator's response is compared to the
 * expected payload as <em>JSON-tree-equivalent</em> — same comparison
 * helper as {@link GscPushBetAggregatorTest}.
 *
 * <h2>Production has 0 events on {@code transfer} in last 30 days</h2>
 * Verified: {@code TransferProcess} is dead code under current traffic
 * (noted in the Phase 3b implementer brief). The fixtures are therefore
 * <em>synthetic</em> — built from the legacy code's documented request
 * shape ({@code TransferRequest.java}) rather than captured from
 * {@code gsc_event_log}. Fixture 01 / 02 exercise the BET (DEBIT) and
 * SETTLED (CREDIT) action-direction branches; fixtures 03 / 04 / 05
 * exercise the signature-error, unknown-currency, and missing-required-
 * parameters short-circuits respectively.
 *
 * <h2>What's exercised</h2>
 * <ul>
 *   <li>{@code 01} — BET action (DEBIT direction). Member doesn't
 *       exist → batch-abort → empty {@code BalanceResponse} shape.
 *       Confirms the dispatch path reaches {@code doDebit}.</li>
 *   <li>{@code 02} — SETTLED action (CREDIT direction; the legacy
 *       {@code actionReward} bug would route this through {@code bet()}
 *       and DEBIT incorrectly). Member doesn't exist → batch-abort.
 *       Confirms the dispatch path reaches {@code doCredit}.</li>
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
 * <p>The action-direction map is also unit-tested directly in
 * {@link #directionMapMatchesGscApiContract} so the spec table in the
 * class doc is structurally verified, not just exercised end-to-end.
 *
 * <p>A separate {@link #productionPayloadsParseWithoutThrowing} test
 * replays raw payloads with realistic-but-synthetic shapes (production
 * is empty so no real captures available) to verify the parser doesn't
 * crash on extreme inputs; those payloads predictably fail signature
 * validation against {@code TEST_SECRET} and surface the SIGNATURE_ERROR
 * shape, which is the desired behavior.
 *
 * <p>Skips with {@code Assume} when no DB password is supplied — same
 * pattern as {@link GscPushBetAggregatorTest}.
 */
public class GscTransferAggregatorTest {

    private static final String TEST_SECRET = "TEST_SECRET";
    private static volatile boolean bootstrapSkipped = false;

    private GscTransferAggregator aggregator;

    @BeforeClass
    public static void bootstrapPool() throws Exception {
        String pw = System.getProperty("test.mysql.password");
        if (pw == null || pw.isEmpty()) pw = System.getenv("MYSQL_PASSWORD");
        if (pw == null || pw.isEmpty()) {
            bootstrapSkipped = true;
            System.out.println("GscTransferAggregatorTest: password not supplied; tests will be skipped");
            return;
        }

        URL propsUrl = GscTransferAggregatorTest.class
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
            props.store(fos, "Auto-generated by GscTransferAggregatorTest — password injected at test time");
        }

        ConnectionPool.getInstance();
    }

    @AfterClass
    public static void cleanUp() {
        if (bootstrapSkipped) return;
        try {
            URL propsUrl = GscTransferAggregatorTest.class
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
            System.err.println("GscTransferAggregatorTest: could not restore placeholder (non-fatal): "
                    + e.getMessage());
        }
    }

    /**
     * Build the aggregator with a stub config. The aggregator constructor
     * itself doesn't touch the DB — only the per-test {@code handle()}
     * invocations do (and only when the dispatch path reaches
     * {@code doDebit}/{@code doCredit}/{@code doReadBalance}). The
     * direction-map unit test below never invokes {@code handle()} so it
     * runs even when the DB pool is not available.
     */
    @Before
    public void setUp() {
        aggregator = new GscTransferAggregator(new GscConfigProvider() {
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
     * before any {@code doDebit}/{@code doCredit} call and run
     * unconditionally.
     */
    @Test public void fixture01_bet_debitPath() throws Exception {
        Assume.assumeFalse("DB password not supplied", bootstrapSkipped);
        runFixture("01");
    }
    @Test public void fixture02_settled_creditPath() throws Exception {
        Assume.assumeFalse("DB password not supplied", bootstrapSkipped);
        runFixture("02");
    }
    @Test public void fixture03_invalidSignature()           throws Exception { runFixture("03"); }
    @Test public void fixture04_unknownCurrency_balanceFallback() throws Exception { runFixture("04"); }
    @Test public void fixture05_missingRequiredParameters()  throws Exception { runFixture("05"); }

    /**
     * Direct unit test of the action-direction map. Mirror of the
     * documented table in {@link GscTransferAggregator}'s class
     * javadoc — this test is the structural guarantee that the
     * mapping doesn't drift silently when someone edits
     * {@code directionFor}. Includes at least one DEBIT, at least one
     * CREDIT, and the explicit NO-OP cases (ROLLBACK and ADJUSTMENT).
     */
    @Test
    public void directionMapMatchesGscApiContract() {
        // DEBIT actions
        assertEquals(GscTransferAggregator.Direction.DEBIT,
                GscTransferAggregator.directionFor("BET"));
        assertEquals(GscTransferAggregator.Direction.DEBIT,
                GscTransferAggregator.directionFor("TIP"));
        assertEquals(GscTransferAggregator.Direction.DEBIT,
                GscTransferAggregator.directionFor("BET_PRESERVE"));

        // CREDIT actions (these all go through actionReward in legacy,
        // which DEBITS due to the bug — the aggregator must NOT
        // preserve the bug).
        assertEquals(GscTransferAggregator.Direction.CREDIT,
                GscTransferAggregator.directionFor("FREEBET"));
        assertEquals(GscTransferAggregator.Direction.CREDIT,
                GscTransferAggregator.directionFor("SETTLED"));
        assertEquals(GscTransferAggregator.Direction.CREDIT,
                GscTransferAggregator.directionFor("CANCEL"));
        assertEquals(GscTransferAggregator.Direction.CREDIT,
                GscTransferAggregator.directionFor("JACKPOT"));
        assertEquals(GscTransferAggregator.Direction.CREDIT,
                GscTransferAggregator.directionFor("BONUS"));
        assertEquals(GscTransferAggregator.Direction.CREDIT,
                GscTransferAggregator.directionFor("PROMO"));
        assertEquals(GscTransferAggregator.Direction.CREDIT,
                GscTransferAggregator.directionFor("LEADERBOARD"));
        assertEquals(GscTransferAggregator.Direction.CREDIT,
                GscTransferAggregator.directionFor("PRESERVE_REFUND"));

        // NO-OP actions (intentionally skip wallet movement)
        assertEquals(GscTransferAggregator.Direction.NOOP,
                GscTransferAggregator.directionFor("ROLLBACK"));
        assertEquals(GscTransferAggregator.Direction.NOOP,
                GscTransferAggregator.directionFor("ADJUSTMENT"));

        // Defensive: null and unknown action → NO-OP (don't move money).
        assertEquals(GscTransferAggregator.Direction.NOOP,
                GscTransferAggregator.directionFor(null));
        assertEquals(GscTransferAggregator.Direction.NOOP,
                GscTransferAggregator.directionFor("BOGUS_ACTION"));
    }

    /**
     * Replay 3 raw production-shape payloads (sign+request_time as one
     * could plausibly emit them) and verify the parser doesn't choke
     * or crash. Production has 0 events in the last 30 days for the
     * {@code transfer} endpoint so the payloads below are synthetic
     * but match the documented {@code TransferRequest} schema —
     * sufficient to exercise the parser surface. The captured signs
     * predictably won't validate against {@link #TEST_SECRET} so the
     * aggregator returns the SIGNATURE_ERROR shape — verifying the
     * no-crash invariant + the legacy {@code "Invalid sign"} response
     * shape.
     */
    @Test
    public void productionPayloadsParseWithoutThrowing() throws Exception {
        String[] payloads = {
                "{\"sign\":\"99d8185c4d7c3939bc9c232fc383b0da\",\"currency\":\"KRW\",\"request_time\":\"1777706702\",\"operator_code\":\"G7A1\",\"member_account\":\"khongduong\",\"product_code\":1002,\"game_type\":\"LIVE_CASINO\",\"transactions\":[{\"id\":\"PROD-1\",\"action\":\"BET\",\"product_code\":1002,\"bet_amount\":\"5000\",\"wager_code\":\"WPROD-1\"},{\"id\":\"PROD-1S\",\"action\":\"SETTLED\",\"product_code\":1002,\"bet_amount\":\"7500\",\"wager_code\":\"WPROD-1\"}]}",
                "{\"sign\":\"36e5bfd072b758d07a7ceb503d31d505\",\"currency\":\"VND\",\"request_time\":\"1777696496\",\"operator_code\":\"G7A1\",\"member_account\":\"sunkrvip999\",\"product_code\":1091,\"game_type\":\"LIVE_CASINO\",\"transactions\":[{\"id\":\"PROD-2\",\"action\":\"BET\",\"product_code\":1091,\"bet_amount\":\"100000\",\"wager_code\":\"WPROD-2\"}]}",
                "{\"sign\":\"2a676c3c6c7f890f82847a9beb4e5502\",\"currency\":\"KRW\",\"request_time\":\"1777705550\",\"operator_code\":\"G7A1\",\"member_account\":\"hayquamnoi\",\"product_code\":1002,\"game_type\":\"LIVE_CASINO\",\"transactions\":[{\"id\":\"PROD-3\",\"action\":\"JACKPOT\",\"product_code\":1002,\"bet_amount\":\"250\",\"wager_code\":\"WPROD-3\"}]}"
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

    // ─────────────────────────────────────────────────────────────────
    // Helpers
    // ─────────────────────────────────────────────────────────────────

    private void runFixture(String name) throws Exception {
        String reqPath = "seamless/gsc/transfer/" + name + "_request.json";
        String respPath = "seamless/gsc/transfer/" + name + "_response.json";
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
        try (InputStream is = GscTransferAggregatorTest.class
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
     * {@link GscPushBetAggregatorTest#assertJsonEquals}.
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
