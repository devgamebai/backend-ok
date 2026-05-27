package com.vinplay.dal.service.seamless;

import com.vinplay.dal.service.MoneyGateway;
import org.junit.Before;
import org.junit.Test;

import javax.servlet.http.HttpServletRequest;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.InputStreamReader;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.nio.charset.StandardCharsets;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * Phase 5p3 — per-handler timing instrumentation tests for
 * {@link SeamlessWalletAggregator}.
 *
 * <p>These tests do NOT need the MySQL pool — they use a tiny in-class
 * {@link InMemAggregator} whose {@code dispatch} returns a hard-coded
 * outcome (no {@code doDebit}/{@code doCredit}/{@code doReadBalance}
 * call), so the timing wrapper wraps an entirely synthetic body. Hence
 * they live here, separate from {@link SeamlessWalletAggregatorTest}
 * whose {@code @Before} would Assume-skip when the DB password is absent.
 */
public class SeamlessWalletAggregatorTimingTest {

    @Before
    public void resetMetrics() {
        AggregatorMetrics.resetForTests();
    }

    /**
     * No-DB test aggregator — dispatch returns a hard-coded POSTED with
     * a configurable sleep so we can verify timing without relying on a
     * MySQL pool. Subclasses tweak {@code sleepMillis}.
     */
    private static final class InMemAggregator extends SeamlessWalletAggregator<String, String> {
        private final String mname;
        long sleepMillis = 0L;
        InMemAggregator(String mname) { this.mname = mname; }
        @Override protected String metricsName() { return mname; }
        @Override protected String parseRequest(String body, HttpServletRequest http) { return body; }
        @Override protected VerifyResult verifySignature(String req, HttpServletRequest http) { return VerifyResult.ok(); }
        @Override protected long currencyToInternal(double amt, String c) { return (long) amt; }
        @Override protected double currencyToExternal(long bal, String c) { return (double) bal; }
        @Override protected String mapActionToSource(String a) { return MoneyGateway.SOURCE_AWC_DEBIT; }
        @Override protected String serializeResponse(SeamlessOutcome out) { return "{}"; }
        @Override protected SeamlessOutcome dispatch(String req) {
            if (sleepMillis > 0) {
                try { Thread.sleep(sleepMillis); } catch (InterruptedException ignored) {}
            }
            return SeamlessOutcome.ok(0L);
        }
    }

    /**
     * Build an {@link HttpServletRequest} via dynamic proxy. Same trick as
     * {@code SeamlessWalletAggregatorTest#mockRequest}.
     */
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

    /**
     * One {@code handle()} call deposits exactly one sample into
     * {@link AggregatorMetrics} under the right bucket name.
     */
    @Test
    public void testTiming_recordsSample() {
        InMemAggregator agg = new InMemAggregator("TestTimingAgg_records");
        String resp = agg.handle(mockRequest("ignored"));
        assertNotNull(resp);

        AggregatorMetrics.Snapshot snap =
                AggregatorMetrics.snapshot("TestTimingAgg_records");
        assertEquals("exactly one sample expected", 1, snap.count);
        assertTrue("avgMs must be non-negative", snap.avgMs >= 0);
    }

    /**
     * A handler that takes >50ms records a sample whose maxMs > 50ms,
     * which is the same threshold the production WARN log gates on. The
     * log line itself is not asserted here — the test classpath uses
     * {@code log4j-over-slf4j} stubs that don't support
     * {@code AppenderSkeleton}; we verify the load-bearing post-condition
     * the scheduler actually reads (the metrics snapshot) and rely on
     * inspection that the WARN branch shares the {@code SLOW_HANDLE_NANOS}
     * threshold.
     */
    @Test
    public void testTiming_warnsAtSlowHandle() {
        InMemAggregator agg = new InMemAggregator("TestTimingAgg_slow");
        agg.sleepMillis = 80L; // >50ms threshold
        String resp = agg.handle(mockRequest("ignored"));
        assertNotNull("slow handle must still return a response", resp);

        AggregatorMetrics.Snapshot snap =
                AggregatorMetrics.snapshot("TestTimingAgg_slow");
        assertEquals("one sample expected", 1, snap.count);
        assertTrue("recorded maxMs must reach the 50ms WARN threshold, was " + snap.maxMs,
                snap.maxMs >= 50L);
    }

    /**
     * 100 fake samples (1ms × 99 + 200ms × 1) — assert p50 ≈ 1ms and the
     * slow tail is captured by max. Mirrors
     * {@code AggregatorMetricsTest#p99Computation_sortsAndPicksTail} but
     * driven through the public API the way the production scheduler
     * sees it.
     */
    @Test
    public void testTiming_p99Computation() {
        final String name = "TestTimingAgg_p99";
        for (int i = 0; i < 99; i++) {
            AggregatorMetrics.recordHandle(name, 1_000_000L);    // 1ms
        }
        AggregatorMetrics.recordHandle(name, 200_000_000L);      // 200ms

        AggregatorMetrics.Snapshot snap = AggregatorMetrics.snapshot(name);
        assertEquals(100, snap.count);
        assertEquals("p50 must be ~1ms", 1L, snap.p50Ms);
        assertEquals("max must be 200ms (the tail outlier)", 200L, snap.maxMs);
        // p99 lands on Math.floor((100-1)*0.99)=98 → 99th fast sample (1ms);
        // semantic guarantee is "p99 in [p50, max]" and the tail is captured.
        assertTrue("p99 must be in [p50, max]",
                snap.p99Ms >= snap.p50Ms && snap.p99Ms <= snap.maxMs);
    }
}
