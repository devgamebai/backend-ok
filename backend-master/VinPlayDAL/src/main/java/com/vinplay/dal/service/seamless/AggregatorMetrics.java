package com.vinplay.dal.service.seamless;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.ConcurrentMap;

/**
 * Phase 5p3 — per-aggregator handler-latency metrics holder.
 *
 * <p>Records how long each {@link SeamlessWalletAggregator#handle} call
 * takes, aggregated by {@link SeamlessWalletAggregator#metricsName()}.
 * The companion {@code AggregatorP99Scheduler} polls these every 60s and
 * fires a Telegram alert when the rolling 1-minute p99 of any aggregator
 * exceeds 100ms — surfaces Mongo-backoff blowups / pool saturation in
 * seconds instead of waiting for end-user complaints.
 *
 * <h2>Design — why a per-aggregator deque, not a histogram</h2>
 * Codebase has no existing metrics library (no Micrometer, no Dropwizard).
 * Bringing one in for a single use-case is overkill; instead this class
 * exposes the bare minimum needed by the scheduler:
 * <ul>
 *   <li>{@link #recordHandle(String, long)} — appends one sample, prunes
 *       expired ones, drops oldest if the deque is full</li>
 *   <li>{@link #snapshot(String)} — copies the deque into a sorted
 *       array, computes count/avg/p50/p99/max, returns immutable struct</li>
 * </ul>
 *
 * <h2>Thread-safety</h2>
 * <ul>
 *   <li>Top-level map: {@link ConcurrentHashMap} (no global lock)</li>
 *   <li>Per-aggregator deque: {@link ConcurrentLinkedDeque} — record path
 *       is wait-free for the common case (no pruning needed); pruning
 *       and bound-enforcement use the deque's own thread-safe pollFirst</li>
 *   <li>Snapshot copies via {@link Iterator}, which on
 *       {@code ConcurrentLinkedDeque} is weakly consistent — concurrent
 *       record/prune do not throw, the snapshot may include or omit
 *       in-flight samples</li>
 * </ul>
 *
 * <h2>Bounded memory</h2>
 * Per-aggregator deque is capped at {@link #MAX_SAMPLES_PER_AGG}. Burst
 * load that exceeds 10000 samples in a 60s window will start dropping the
 * OLDEST samples — favouring p99 freshness over completeness, which is
 * what we want for "is it slow right now?" alerting.
 */
public final class AggregatorMetrics {

    /** Rolling window — anything older than this is pruned on each record. */
    static final long WINDOW_NANOS = 60L * 1_000_000_000L;

    /** Per-aggregator deque cap (drop-oldest above this). */
    static final int MAX_SAMPLES_PER_AGG = 10_000;

    private static final ConcurrentMap<String, ConcurrentLinkedDeque<TimedSample>> BY_NAME =
            new ConcurrentHashMap<String, ConcurrentLinkedDeque<TimedSample>>();

    private AggregatorMetrics() {}

    /** One handle() invocation's timing point. */
    static final class TimedSample {
        final long elapsedNanos;
        final long sampleAtNanos;
        TimedSample(long elapsedNanos, long sampleAtNanos) {
            this.elapsedNanos = elapsedNanos;
            this.sampleAtNanos = sampleAtNanos;
        }
    }

    /**
     * Record a single {@code handle()} duration. Cheap path:
     * one CHM lookup, one deque append, occasional poll. Allocation: one
     * {@code TimedSample}. We accept that — the alternative (a primitive
     * ring buffer) costs more code and a CAS loop on every write.
     *
     * @param aggregatorName the {@link SeamlessWalletAggregator#metricsName()}
     *                       — must be stable per-aggregator, used as the map key
     * @param durationNanos  measured duration; pass exactly what
     *                       {@code System.nanoTime()} delta returns
     */
    public static void recordHandle(String aggregatorName, long durationNanos) {
        if (aggregatorName == null) return;
        ConcurrentLinkedDeque<TimedSample> deque = BY_NAME.get(aggregatorName);
        if (deque == null) {
            deque = new ConcurrentLinkedDeque<TimedSample>();
            ConcurrentLinkedDeque<TimedSample> existing =
                    BY_NAME.putIfAbsent(aggregatorName, deque);
            if (existing != null) deque = existing;
        }
        long now = System.nanoTime();
        deque.add(new TimedSample(durationNanos, now));

        // Prune expired (oldest first). ConcurrentLinkedDeque iteration is
        // weakly consistent; peekFirst is cheap and correct under contention.
        long cutoff = now - WINDOW_NANOS;
        TimedSample head;
        while ((head = deque.peekFirst()) != null && head.sampleAtNanos < cutoff) {
            // Race: another thread may have already polled the head; that's
            // fine — pollFirst returns null and we exit.
            if (!deque.removeFirstOccurrence(head)) break;
        }

        // Bound: drop oldest if we're over cap. Approximate; we don't take
        // a global lock to make this exact — under burst we may dip slightly
        // over, but never unbounded.
        int over = deque.size() - MAX_SAMPLES_PER_AGG;
        while (over-- > 0 && deque.pollFirst() != null) { /* drop */ }
    }

    /**
     * Compute count / avg / p50 / p99 / max for the rolling 1-minute
     * window. Allocates one array of size = current deque size; the
     * scheduler calls this once per aggregator per minute, so the cost is
     * negligible.
     *
     * @return an immutable snapshot; never null
     */
    public static Snapshot snapshot(String aggregatorName) {
        ConcurrentLinkedDeque<TimedSample> deque = BY_NAME.get(aggregatorName);
        if (deque == null) return Snapshot.empty(aggregatorName);

        long cutoff = System.nanoTime() - WINDOW_NANOS;
        // Copy non-expired samples into a primitive array. Iterator is
        // weakly consistent; concurrent recordHandle is harmless.
        List<Long> in = new ArrayList<Long>(Math.min(deque.size(), MAX_SAMPLES_PER_AGG));
        for (Iterator<TimedSample> it = deque.iterator(); it.hasNext(); ) {
            TimedSample s = it.next();
            if (s.sampleAtNanos >= cutoff) in.add(s.elapsedNanos);
        }
        if (in.isEmpty()) return Snapshot.empty(aggregatorName);

        long[] arr = new long[in.size()];
        long sum = 0L;
        for (int i = 0; i < arr.length; i++) {
            arr[i] = in.get(i);
            sum += arr[i];
        }
        Arrays.sort(arr);

        long avgNanos = sum / arr.length;
        long p50 = arr[(int) Math.floor((arr.length - 1) * 0.50)];
        long p99 = arr[(int) Math.floor((arr.length - 1) * 0.99)];
        long max = arr[arr.length - 1];

        return new Snapshot(
                aggregatorName,
                arr.length,
                nanosToMillis(avgNanos),
                nanosToMillis(p50),
                nanosToMillis(p99),
                nanosToMillis(max));
    }

    /** Set of aggregator names that have recorded at least one sample. */
    public static Set<String> registeredNames() {
        return Collections.unmodifiableSet(BY_NAME.keySet());
    }

    /** Test-only — reset all state. Not for production use. */
    static void resetForTests() {
        BY_NAME.clear();
    }

    private static long nanosToMillis(long nanos) {
        // Round to nearest ms for human-readable alert text. Only for
        // display; internal storage stays in nanos.
        return (nanos + 500_000L) / 1_000_000L;
    }

    /**
     * Immutable view of an aggregator's recent timing distribution.
     * Times are in milliseconds (rounded to nearest, suitable for log /
     * alert text). All fields are 0 for {@link #empty(String)}.
     */
    public static final class Snapshot {
        public final String aggregatorName;
        public final int count;
        public final long avgMs;
        public final long p50Ms;
        public final long p99Ms;
        public final long maxMs;

        Snapshot(String aggregatorName, int count, long avgMs, long p50Ms, long p99Ms, long maxMs) {
            this.aggregatorName = aggregatorName;
            this.count = count;
            this.avgMs = avgMs;
            this.p50Ms = p50Ms;
            this.p99Ms = p99Ms;
            this.maxMs = maxMs;
        }

        static Snapshot empty(String aggregatorName) {
            return new Snapshot(aggregatorName, 0, 0L, 0L, 0L, 0L);
        }

        @Override
        public String toString() {
            return aggregatorName
                    + " count=" + count
                    + " avgMs=" + avgMs
                    + " p50Ms=" + p50Ms
                    + " p99Ms=" + p99Ms
                    + " maxMs=" + maxMs;
        }
    }
}
