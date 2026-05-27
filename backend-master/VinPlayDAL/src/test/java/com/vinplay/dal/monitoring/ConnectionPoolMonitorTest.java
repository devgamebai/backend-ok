package com.vinplay.dal.monitoring;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Phase 5 prep gate 5p4 — unit tests for the pool-utilization monitor.
 *
 * <p>Two pieces are exercised independently:
 * <ul>
 *   <li>{@link PoolPressureTracker} — pure logic test, no DB / no
 *       scheduler needed. This is the load-bearing test for the
 *       "2 consecutive samples above 80%" alert policy.</li>
 *   <li>{@link ConnectionPoolMonitor#sampleOnce()} — runs against
 *       whatever {@code ConnectionPool} state exists at test time.
 *       In CI without a DB password the pool init throws, so the
 *       monitor returns the {@code active = -1} unavailable
 *       placeholder. The test asserts the placeholder shape so we
 *       prove the "never throws" contract holds even on a cold JVM.</li>
 * </ul>
 *
 * <p>No DB connection is required to make this test green — by design.
 * The integration of {@code sampleOnce()} against a live HikariCP pool
 * is implicitly covered the moment the scheduler starts inside
 * {@code VinPlayBackendMain}; this unit test just guarantees the
 * fall-through paths don't blow up.
 */
public class ConnectionPoolMonitorTest {

    // ──────────────────────────────────────────────────────────────────
    // PoolPressureTracker — consecutive-counter logic
    // ──────────────────────────────────────────────────────────────────

    @Test
    public void tracker_singleHighSample_doesNotAlert() {
        PoolPressureTracker t = new PoolPressureTracker(80.0, 2);
        assertFalse("first high sample must not alert", t.recordSample(90.0));
        assertEquals(1, t.currentStreak());
    }

    @Test
    public void tracker_twoConsecutiveHighSamples_fireOnce() {
        PoolPressureTracker t = new PoolPressureTracker(80.0, 2);
        assertFalse(t.recordSample(85.0));
        assertTrue("second consecutive high sample must alert", t.recordSample(90.0));
    }

    @Test
    public void tracker_resetsAfterAlert_requiresAnotherFullStreak() {
        PoolPressureTracker t = new PoolPressureTracker(80.0, 2);
        assertFalse(t.recordSample(85.0));
        assertTrue(t.recordSample(90.0));     // alerts and resets
        assertEquals("streak reset to 0 after alert", 0, t.currentStreak());
        assertFalse("immediate next high sample is streak=1, no alert", t.recordSample(95.0));
        assertTrue("second consecutive high sample alerts again", t.recordSample(95.0));
    }

    @Test
    public void tracker_lowSampleResetsStreak() {
        PoolPressureTracker t = new PoolPressureTracker(80.0, 2);
        assertFalse(t.recordSample(85.0));
        assertEquals(1, t.currentStreak());
        assertFalse("low sample must reset streak", t.recordSample(50.0));
        assertEquals(0, t.currentStreak());
        assertFalse("now back to streak=1 after one high sample", t.recordSample(85.0));
        assertEquals(1, t.currentStreak());
    }

    @Test
    public void tracker_thresholdIsExclusive_exactly80DoesNotCount() {
        // Spec: ">80%" — exactly 80% must NOT alert. This pins the boundary.
        PoolPressureTracker t = new PoolPressureTracker(80.0, 2);
        assertFalse(t.recordSample(80.0));
        assertFalse(t.recordSample(80.0));
        assertEquals(0, t.currentStreak());
    }

    @Test
    public void tracker_threeConsecutiveThreshold() {
        // Sanity: the policy is tunable. Verify with N=3.
        PoolPressureTracker t = new PoolPressureTracker(50.0, 3);
        assertFalse(t.recordSample(60.0));
        assertFalse(t.recordSample(60.0));
        assertTrue(t.recordSample(60.0));
        assertEquals(0, t.currentStreak());
    }

    @Test
    public void tracker_resetClearsStreak() {
        PoolPressureTracker t = new PoolPressureTracker(80.0, 5);
        t.recordSample(95.0);
        t.recordSample(95.0);
        assertEquals(2, t.currentStreak());
        t.reset();
        assertEquals(0, t.currentStreak());
    }

    // ──────────────────────────────────────────────────────────────────
    // ConnectionPoolMonitor.sampleOnce() — never-throws contract
    // ──────────────────────────────────────────────────────────────────

    @Test
    public void sampleOnce_unknownPool_returnsUnavailablePlaceholder() {
        // Asking for a pool name that definitely doesn't exist in the
        // ConnectionPool registry must NOT throw, and must return the
        // active=-1 sentinel so the scheduler can WARN-skip.
        ConnectionPoolMonitor.SampleResult s = ConnectionPoolMonitor.sampleOnce(
                "definitely-not-a-real-pool-" + System.nanoTime());
        assertFalse("unknown pool must surface as unavailable", s.isAvailable());
        assertEquals(-1, s.active);
        assertEquals(-1, s.idle);
        assertEquals(-1, s.total);
        assertEquals(-1, s.maxPoolSize);
        assertEquals(0.0, s.utilizationPct, 0.0001);
    }

    @Test
    public void sampleOnce_neverThrows_evenWithoutInitializedPool() {
        // Repeated calls on the default user pool must never throw —
        // even when the pool isn't yet initialized in the test JVM.
        // (If the pool happens to be initialized via some other test's
        // side effect, this still passes — we only assert "no throw".)
        for (int i = 0; i < 5; i++) {
            ConnectionPoolMonitor.SampleResult s = ConnectionPoolMonitor.sampleOnce();
            // Either the pool is up (active >= 0, max > 0) or it's
            // unavailable. Both shapes are acceptable here.
            if (s.isAvailable()) {
                assertTrue("active >= 0 when available", s.active >= 0);
                assertTrue("maxPoolSize > 0 when available", s.maxPoolSize > 0);
                assertTrue("utilization in [0,100+]", s.utilizationPct >= 0.0);
            } else {
                assertEquals(-1, s.active);
            }
        }
    }

    @Test
    public void sampleResult_toString_isReadable() {
        ConnectionPoolMonitor.SampleResult ok =
                new ConnectionPoolMonitor.SampleResult("mysqlpoolname", 24, 6, 30, 30, 80.0);
        String s = ok.toString();
        assertTrue("includes pool name", s.contains("mysqlpoolname"));
        assertTrue("includes active count", s.contains("active=24"));
        assertTrue("includes max", s.contains("max=30"));
        assertTrue("includes util%", s.contains("80.0%"));

        ConnectionPoolMonitor.SampleResult bad =
                ConnectionPoolMonitor.SampleResult.unavailable("mysqlpoolname");
        assertTrue("placeholder labels itself unavailable",
                bad.toString().contains("UNAVAILABLE"));
    }
}
