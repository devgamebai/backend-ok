package com.vinplay.bancareplay;

import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * Mock-Redis end-to-end test for {@link BancaReplayWorker#tick()}.
 *
 * <p>Scenario (per executor brief):
 * <ul>
 *   <li>5 items on {@code banca:failed_settle}</li>
 *   <li>2 succeed on first replay (HTTP 200, success:true)</li>
 *   <li>2 fail with 4xx → moved to {@code banca:failed_settle_dead}</li>
 *   <li>1 fails with 5xx → re-pushed into a retry bucket with delay</li>
 * </ul>
 */
public class BancaReplayWorkerTest {

    private FakeRedis redis;
    private ScriptedHttp http;
    private RecordingSink sink;

    @Before
    public void setUp() {
        redis = new FakeRedis();
        http = new ScriptedHttp();
        sink = new RecordingSink();
    }

    @Test
    public void tick_mixedOutcomes_routesEachItemCorrectly() {
        // 5 distinct payloads, distinguished by external_ref.
        String ok1 = makePayload(11, "OK1");
        String ok2 = makePayload(12, "OK2");
        String bad1 = makePayload(21, "BAD1");
        String bad2 = makePayload(22, "BAD2");
        String tx5 = makePayload(31, "TX5");

        redis.rpush(BancaReplayWorker.LIST_PENDING, ok1);
        redis.rpush(BancaReplayWorker.LIST_PENDING, ok2);
        redis.rpush(BancaReplayWorker.LIST_PENDING, bad1);
        redis.rpush(BancaReplayWorker.LIST_PENDING, bad2);
        redis.rpush(BancaReplayWorker.LIST_PENDING, tx5);

        // Script responses keyed by external_ref.
        http.script.put("OK1", new BancaReplayWorker.HttpResponse(200, "{\"success\":true,\"balance_after_vnd\":1000}"));
        http.script.put("OK2", new BancaReplayWorker.HttpResponse(200, "{\"success\":true,\"balance_after_vnd\":2000}"));
        http.script.put("BAD1", new BancaReplayWorker.HttpResponse(200, "{\"success\":false,\"errorCode\":\"4004\",\"message\":\"insufficient balance\"}"));
        http.script.put("BAD2", new BancaReplayWorker.HttpResponse(400, "{\"success\":false,\"errorCode\":\"4001\",\"message\":\"bad request\"}"));
        http.script.put("TX5", new BancaReplayWorker.HttpResponse(503, "service unavailable"));

        BancaReplayWorker worker = new BancaReplayWorker(
                redis, http, sink,
                /* batchSize */ 100,
                /* maxAttempts */ 8,
                /* intervalSec */ 30,
                /* initialBackoffMs */ 1000L);

        worker.tick();

        // 2 successes drained, 2 dead, 1 rescheduled
        assertEquals(2, worker.getTotalSucceeded());
        assertEquals(2, worker.getTotalToDead());
        assertEquals(1, worker.getTotalRescheduled());
        assertEquals(5, worker.getTotalReplayed());

        // Pending list now empty
        assertTrue(redis.lists.getOrDefault(BancaReplayWorker.LIST_PENDING,
                new ArrayDeque<>()).isEmpty());

        // Dead list has the two terminal items
        Deque<String> dead = redis.lists.get(BancaReplayWorker.LIST_DEAD);
        assertNotNull("dead list should exist", dead);
        assertEquals(2, dead.size());
        for (String d : dead) {
            assertTrue("dead item must be one of the BAD payloads, was: " + d,
                    d.contains("BAD1") || d.contains("BAD2"));
        }

        // The 5xx item lives in some retry bucket (key starts with the prefix).
        boolean foundBucket = false;
        for (String k : redis.lists.keySet()) {
            if (!k.startsWith(BancaReplayWorker.RETRY_BUCKET_PREFIX)) continue;
            for (String v : redis.lists.get(k)) {
                if (v.contains("TX5")) {
                    foundBucket = true;
                    // attempt counter should have been bumped to 1
                    assertTrue("attempt counter should be set on rescheduled item",
                            v.contains("\"_replay_attempt\":1"));
                }
            }
        }
        assertTrue("rescheduled 5xx item should live in a retry bucket", foundBucket);

        // Anomaly sink got a warn row because dead-list grew this tick
        assertEquals(1, sink.rows.size());
        assertEquals("WARN", sink.rows.get(0).severity);
        assertEquals("BANCA_FAILED_SETTLE_DEAD", sink.rows.get(0).invariant);
    }

    @Test
    public void promoteDueBuckets_movesElapsedEntriesBack() {
        long now = 1_700_000_000_000L;
        String dueKey   = BancaReplayWorker.RETRY_BUCKET_PREFIX + ((now / 1000L) - 5); // 5s ago
        String futureKey = BancaReplayWorker.RETRY_BUCKET_PREFIX + ((now / 1000L) + 60); // 1m future

        redis.rpush(dueKey, "{\"external_ref\":\"banca:settle:42:past:1\"}");
        redis.rpush(dueKey, "{\"external_ref\":\"banca:settle:42:past:2\"}");
        redis.rpush(futureKey, "{\"external_ref\":\"banca:settle:42:future:1\"}");

        BancaReplayWorker worker = new BancaReplayWorker(
                redis, http, sink, 100, 8, 30, 1000L);

        worker.promoteDueBuckets(now);

        // Due bucket drained back into pending
        Deque<String> pending = redis.lists.get(BancaReplayWorker.LIST_PENDING);
        assertNotNull(pending);
        assertEquals(2, pending.size());
        // Due key deleted
        assertFalse("due bucket should be deleted", redis.lists.containsKey(dueKey));
        // Future bucket intact
        Deque<String> future = redis.lists.get(futureKey);
        assertNotNull(future);
        assertEquals(1, future.size());
    }

    @Test
    public void maxAttempts_movesItemToDeadList() {
        // Item already at attempt=8, next 5xx pushes it to dead, not retry.
        String item = "{\"external_ref\":\"banca:settle:99:forever:1\",\"_replay_attempt\":8}";
        redis.rpush(BancaReplayWorker.LIST_PENDING, item);
        http.script.put("forever", new BancaReplayWorker.HttpResponse(503, "down"));

        BancaReplayWorker worker = new BancaReplayWorker(
                redis, http, sink, 100, /* maxAttempts */ 8, 30, 1000L);

        worker.tick();

        assertEquals(1, worker.getTotalToDead());
        assertEquals(0, worker.getTotalRescheduled());
        Deque<String> dead = redis.lists.get(BancaReplayWorker.LIST_DEAD);
        assertNotNull(dead);
        assertEquals(1, dead.size());
    }

    @Test
    public void unparseableJson_goesStraightToDeadList() {
        redis.rpush(BancaReplayWorker.LIST_PENDING, "this is not json");

        BancaReplayWorker worker = new BancaReplayWorker(
                redis, http, sink, 100, 8, 30, 1000L);
        worker.tick();

        assertEquals(1, worker.getTotalToDead());
        Deque<String> dead = redis.lists.get(BancaReplayWorker.LIST_DEAD);
        assertNotNull(dead);
        assertEquals(1, dead.size());
        assertEquals("this is not json", dead.peek());
    }

    // -----------------------------------------------------------------
    // Fakes
    // -----------------------------------------------------------------

    /** In-memory list-only Redis fake. Sufficient for the worker's surface. */
    static final class FakeRedis implements BancaReplayWorker.RedisOps {
        final Map<String, Deque<String>> lists = new LinkedHashMap<>();

        @Override public String lpop(String key) {
            Deque<String> q = lists.get(key);
            if (q == null || q.isEmpty()) return null;
            String v = q.pollFirst();
            if (q.isEmpty()) lists.remove(key);
            return v;
        }

        @Override public void rpush(String key, String value) {
            lists.computeIfAbsent(key, k -> new ArrayDeque<>()).addLast(value);
        }

        @Override public void del(String key) {
            lists.remove(key);
        }

        @Override public List<String> scanKeys(String pattern, int limit) {
            // Pattern is "prefix*" only — sufficient for the worker.
            String prefix = pattern.endsWith("*") ? pattern.substring(0, pattern.length() - 1) : pattern;
            List<String> out = new ArrayList<>();
            for (String k : lists.keySet()) {
                if (k.startsWith(prefix)) out.add(k);
                if (out.size() >= limit) break;
            }
            return out;
        }
    }

    /**
     * HTTP fake whose response is selected by a substring of the JSON body
     * (we use external_ref to discriminate). If no key matches, returns
     * a generic 500 so the test fails loudly on bad scripting.
     */
    static final class ScriptedHttp implements BancaReplayWorker.HttpPoster {
        final Map<String, BancaReplayWorker.HttpResponse> script = new HashMap<>();

        @Override public BancaReplayWorker.HttpResponse post(String body) throws IOException {
            for (Map.Entry<String, BancaReplayWorker.HttpResponse> e : script.entrySet()) {
                if (body.contains(e.getKey())) return e.getValue();
            }
            return new BancaReplayWorker.HttpResponse(500, "no script match");
        }
    }

    static final class RecordingSink implements BancaReplayWorker.AnomalySink {
        static final class Row {
            final String severity, invariant, details;
            Row(String s, String i, String d) { severity = s; invariant = i; details = d; }
        }
        final List<Row> rows = new ArrayList<>();
        @Override public void write(String severity, String invariant, String details) {
            rows.add(new Row(severity, invariant, details));
        }
    }

    // -----------------------------------------------------------------
    // Helpers
    // -----------------------------------------------------------------

    private static String makePayload(long userId, String marker) {
        // Payload mirrors what MoneyGatewayClient.cs pushes — keep the
        // _replay_attempt field absent so the worker initializes it.
        return "{\"user_id\":" + userId
                + ",\"amount_milli\":-12345"
                + ",\"tx_type\":\"WAGER_DEBIT_BANCA\""
                + ",\"session_id\":\"" + marker + "\""
                + ",\"external_ref\":\"banca:settle:" + userId + ":" + marker + ":1700000000000\""
                + ",\"checkpoint_ms\":1700000000000"
                + ",\"game_key\":\"banca\"}";
    }

    // Keep the import-set narrow — suppress unused warnings on JDK 8.
    @SuppressWarnings("unused")
    private static List<String> emptyList() { return Collections.emptyList(); }
}
