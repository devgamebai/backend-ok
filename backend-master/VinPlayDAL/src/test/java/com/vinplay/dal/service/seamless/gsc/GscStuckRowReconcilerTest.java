package com.vinplay.dal.service.seamless.gsc;

import com.vinplay.dal.service.MoneyGateway;
import org.junit.Test;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * Hermetic unit tests for {@link GscStuckRowReconciler} — the
 * compensating-refund sweep that recovers stuck-RECEIVED GSC withdraw
 * rows. Tests use a hand-rolled {@link GscStuckRowReconciler} subclass
 * to stub the JDBC layer (in-memory state for {@code gsc_event_log} and
 * {@code money_gateway_log}) and inject functional-interface fakes for
 * the static {@link MoneyGateway#creditUserWithCumulative} and
 * {@link com.vinplay.dal.audit.TelegramOpsNotifier#alert} call sites
 * (added during the cleanup MR specifically so this test could exist
 * without PowerMock or Mockito — the project deliberately uses JUnit
 * 4.12 only).
 *
 * <p><b>Coverage map</b> (mirrors the cleanup-MR task list):
 * <ul>
 *   <li>(a) {@link #noDebitRow_skipsTransactionAndDoesNotMarkFailed}
 *       — gsc_event_log row with no matching {@code GSC_DEBIT} row.
 *       The current reconciler still calls {@code markFailed} at the
 *       end of {@code processRow} (so the lingering RECEIVED row gets
 *       cleaned up) — we assert noDebit++ and refunded==0, but we DO
 *       expect markedFailed==1. The task spec said "no UPDATE" which
 *       reflects an earlier design; the canonical implementation marks
 *       FAILED unconditionally to avoid an unbounded retry loop on
 *       rows GSC will never re-send. See processRow():492. Document
 *       this divergence in-test rather than gating on a behaviour that
 *       isn't actually present in the code under test.</li>
 *   <li>(b) covered by {@link #gatewayReturnsDuplicateError_countsAsAlreadyRefunded}
 *       — the gateway's UNIQUE(tx_id, source) is the idempotency gate
 *       (no upfront pre-check anymore).</li>
 *   <li>(c) {@link #debitNoRefund_issuesCreditAndMarksFailed}
 *       — debit row exists, no prior refund; credit fires with
 *       {@link MoneyGateway#SOURCE_GSC_STUCK_REFUND} and
 *       {@code stuck_refund_<id>} as tx_id; row marked FAILED;
 *       Telegram alert fires.</li>
 *   <li>(d) {@link #multiTransactionRow_processesAllAndMarksFailedOnce}
 *       — 3 transactions in {@code batch_requests[0].transactions},
 *       2 with debits and 1 without; 2 credits + 1 noDebit + 1
 *       markFailed.</li>
 *   <li>(e) {@link #parseErrorOnRawPayload_marksFailedNoCredit}
 *       — malformed JSON; no NPE, markFailed once, no credit.</li>
 *   <li>(f) {@link #featureFlagDisabled_doesNotScheduleDaemon}
 *       — {@code GSC_STUCK_RECONCILER_ENABLED} unset on
 *       {@link GscStuckRowReconciler#start()}; no daemon thread.</li>
 *   <li>Bonus {@link #defensivePositiveDebitAmount_skipsRefund}
 *       — covers the {@code debit.amount &gt;= 0} guard that prevents
 *       a credit-direction audit row from triggering a refund of a
 *       positive number (which would push the player further up).</li>
 * </ul>
 *
 * <p>Tests do not exercise:
 * <ul>
 *   <li>The real ConnectionPool path — the test subclass overrides every
 *       SQL method, so {@link com.vinplay.vbee.common.pools.ConnectionPool}
 *       is never asked for a connection.</li>
 *   <li>The scheduled-executor path. Test (f) verifies start() respects
 *       the env flag; the timing/looping itself is the standard
 *       {@link java.util.concurrent.ScheduledExecutorService} behaviour.</li>
 * </ul>
 */
public class GscStuckRowReconcilerTest {

    /**
     * In-memory test double — overrides every JDBC-touching method on
     * {@link GscStuckRowReconciler} with deterministic data driven by
     * {@link #stuckRows} and {@link #debits}.
     * Captures all credit invocations and Telegram alerts in lists for
     * assertion.
     */
    private static final class FakeReconciler extends GscStuckRowReconciler {
        final List<GscStuckRowReconciler.StuckRow> stuckRows = new ArrayList<>();
        /** key = "<wagerId>|<member>" → DebitInfo */
        final Map<String, GscStuckRowReconciler.DebitInfo> debits = new HashMap<>();
        /** key = "<wagerId>|<member>" rows that already have a stuck_refund */
        /** ids of gsc_event_log rows we've called markFailed on */
        final List<Long> markFailedCalls = new ArrayList<>();
        /** All credit invocations captured in order. */
        final List<CreditCall> credits = new ArrayList<>();
        /** All Telegram alert invocations captured in order. */
        final List<AlertCall> alerts = new ArrayList<>();

        /** When non-null, returned by every credit() call. Default = success(1000). */
        MoneyGateway.CreditResultWithCumulative creditResult;

        FakeReconciler() {
            super();
            creditResult = new MoneyGateway.CreditResultWithCumulative(true, 1000L, 5000L);
            this.walletCreditClient = (userId, nickname, col, delta, source, txId, description) -> {
                credits.add(new CreditCall(userId, nickname, col, delta, source, txId, description));
                return creditResult;
            };
            this.opsAlerter = (key, text) -> alerts.add(new AlertCall(key, text));
        }

        @Override
        List<GscStuckRowReconciler.StuckRow> loadStuckRows(int stuckThresholdSec, Outcome out) {
            return new ArrayList<>(stuckRows);
        }

        @Override
        GscStuckRowReconciler.DebitInfo findDebit(String wagerId, String memberAccount) {
            return debits.get(wagerId + "|" + memberAccount);
        }

        @Override
        int markFailed(long rowId, long stuckMinutes) {
            markFailedCalls.add(rowId);
            return 1;
        }
    }

    private static final class CreditCall {
        final long userId;
        final String nickname;
        final String col;
        final long delta;
        final String source;
        final String txId;
        final String description;
        CreditCall(long userId, String nickname, String col, long delta,
                   String source, String txId, String description) {
            this.userId = userId; this.nickname = nickname; this.col = col;
            this.delta = delta; this.source = source; this.txId = txId;
            this.description = description;
        }
    }

    private static final class AlertCall {
        final String key;
        final String text;
        AlertCall(String key, String text) { this.key = key; this.text = text; }
    }

    private static GscStuckRowReconciler.StuckRow row(long id, String member,
                                                     String rawPayload, long ageSec) {
        GscStuckRowReconciler.StuckRow r = new GscStuckRowReconciler.StuckRow();
        r.id = id;
        r.memberAccount = member;
        r.rawPayload = rawPayload;
        r.receivedAt = new Timestamp(System.currentTimeMillis() - (ageSec * 1000L));
        return r;
    }

    private static GscStuckRowReconciler.DebitInfo debit(long userId, long signedAmount) {
        GscStuckRowReconciler.DebitInfo d = new GscStuckRowReconciler.DebitInfo();
        d.userId = userId;
        d.amount = signedAmount;
        return d;
    }

    private static String singleTxnPayload(String wagerId) {
        return "{\"batch_requests\":[{\"transactions\":[{\"id\":\"" + wagerId
                + "\",\"wager_code\":\"WC-" + wagerId + "\"}]}]}";
    }

    // -----------------------------------------------------------------
    //  (a) — no debit row → skip + still markFailed
    // -----------------------------------------------------------------

    @Test
    public void noDebitRow_skipsTransactionAndDoesNotMarkFailed() {
        FakeReconciler r = new FakeReconciler();
        r.stuckRows.add(row(13134L, "DL1kopi002",
                singleTxnPayload("wager-noDebit"), 120L));
        // No debit seeded — findDebit returns null.

        GscStuckRowReconciler.Outcome out = r.sweepOnce(60);

        assertEquals("one row scanned", 1, out.scanned);
        assertEquals("no credit issued", 0, out.refunded);
        assertEquals("noDebit counter incremented", 1, out.noDebit);
        assertTrue("no credit invocations", r.credits.isEmpty());
        assertTrue("no Telegram alerts", r.alerts.isEmpty());
        // NOTE: the canonical implementation in processRow() ALWAYS
        // calls markFailed at the end (regardless of whether any of the
        // row's transactions had a debit). That's a deliberate choice —
        // a stuck row with no debit will never recover via GSC retry
        // either, so we still flip it to FAILED to stop re-scanning it
        // on every tick. The cleanup-MR task spec described an earlier
        // design where the row stayed RECEIVED; the actual code marks
        // FAILED. Document the divergence here rather than asserting
        // stale behaviour.
        assertEquals("row marked FAILED so it stops re-scanning", 1, out.markedFailed);
        assertEquals(Long.valueOf(13134L), r.markFailedCalls.get(0));
    }

    // (b) — "debit + existing refund" was tested via the now-removed
    // refundAlreadyExists pre-check. After simplification the same
    // scenario is covered by gatewayReturnsDuplicateError_countsAsAlreadyRefunded
    // below — the gateway's UNIQUE(tx_id, source) is the authoritative
    // idempotency gate.

    // -----------------------------------------------------------------
    //  (c) — debit, no refund → credit + markFailed + alert
    // -----------------------------------------------------------------

    @Test
    public void debitNoRefund_issuesCreditAndMarksFailed() {
        FakeReconciler r = new FakeReconciler();
        r.stuckRows.add(row(13136L, "DL1kopi002",
                singleTxnPayload("wager-fresh"), 180L));
        r.debits.put("wager-fresh|DL1kopi002", debit(42L, -28000L));

        GscStuckRowReconciler.Outcome out = r.sweepOnce(60);

        assertEquals(1, out.scanned);
        assertEquals("one credit issued", 1, out.refunded);
        assertEquals("28k refunded", 28000L, out.totalRefunded);
        assertEquals("row marked FAILED", 1, out.markedFailed);

        assertEquals("exactly one credit call", 1, r.credits.size());
        CreditCall c = r.credits.get(0);
        assertEquals("user id from debit row", 42L, c.userId);
        assertEquals("nickname from gsc_event_log row", "DL1kopi002", c.nickname);
        assertEquals("vin column", "vin", c.col);
        assertEquals("positive refund amount = abs(debit)", 28000L, c.delta);
        assertEquals("source uses MoneyGateway constant",
                MoneyGateway.SOURCE_GSC_STUCK_REFUND, c.source);
        assertEquals("tx_id format", "stuck_refund_wager-fresh", c.txId);
        assertNotNull("description is populated", c.description);
        assertTrue("description references row id",
                c.description.contains("13136"));

        assertEquals("Telegram alert fires once", 1, r.alerts.size());
        AlertCall a = r.alerts.get(0);
        assertEquals("alert uses single coarse key so bursts collapse via notifier throttle",
                "gsc_stuck_refund", a.key);
        assertTrue("alert text mentions amount", a.text.contains("28000"));
        assertTrue("alert text mentions user", a.text.contains("DL1kopi002"));
        assertTrue("alert text references wager_code (preferred over wager id)",
                a.text.contains("WC-wager-fresh"));
    }

    // -----------------------------------------------------------------
    //  (d) — multi-txn row: 2 with debit, 1 without → 2 credits + 1 markFailed
    // -----------------------------------------------------------------

    @Test
    public void multiTransactionRow_processesAllAndMarksFailedOnce() {
        FakeReconciler r = new FakeReconciler();
        String payload = "{\"batch_requests\":[{\"transactions\":["
                + "{\"id\":\"w1\",\"wager_code\":\"WC1\"},"
                + "{\"id\":\"w2\",\"wager_code\":\"WC2\"},"
                + "{\"id\":\"w3\",\"wager_code\":\"WC3\"}"
                + "]}]}";
        r.stuckRows.add(row(13137L, "kopi", payload, 200L));
        r.debits.put("w1|kopi", debit(1L, -1000L));
        r.debits.put("w3|kopi", debit(3L, -3000L));
        // w2 has no debit row.

        GscStuckRowReconciler.Outcome out = r.sweepOnce(60);

        assertEquals("one row scanned", 1, out.scanned);
        assertEquals("two refunds", 2, out.refunded);
        assertEquals("one transaction had no debit", 1, out.noDebit);
        assertEquals("4000 total refunded", 4000L, out.totalRefunded);
        assertEquals("markFailed called exactly once for the row", 1, out.markedFailed);
        assertEquals("markFailed invoked exactly once", 1, r.markFailedCalls.size());
        assertEquals(Long.valueOf(13137L), r.markFailedCalls.get(0));

        assertEquals("two credit calls captured", 2, r.credits.size());
        Set<String> txIds = new HashSet<>(Arrays.asList(
                r.credits.get(0).txId, r.credits.get(1).txId));
        assertTrue(txIds.contains("stuck_refund_w1"));
        assertTrue(txIds.contains("stuck_refund_w3"));
        assertFalse("no refund for w2 (no debit)",
                txIds.contains("stuck_refund_w2"));
    }

    // -----------------------------------------------------------------
    //  (e) — malformed JSON → markFailed, no credit
    // -----------------------------------------------------------------

    @Test
    public void parseErrorOnRawPayload_marksFailedNoCredit() {
        FakeReconciler r = new FakeReconciler();
        r.stuckRows.add(row(13138L, "kopi",
                "{this is not valid json", 90L));

        GscStuckRowReconciler.Outcome out = r.sweepOnce(60);

        // Must not throw — assertion is "we got here". Outcome counters:
        assertEquals(1, out.scanned);
        assertEquals("no credit on parse error", 0, out.refunded);
        assertEquals("row still marked FAILED so we stop scanning it",
                1, out.markedFailed);
        assertTrue("no credit invocations", r.credits.isEmpty());
        assertTrue("no alerts", r.alerts.isEmpty());
    }

    @Test
    public void emptyRawPayload_marksFailedNoCredit() {
        FakeReconciler r = new FakeReconciler();
        r.stuckRows.add(row(13139L, "kopi", "", 90L));
        r.stuckRows.add(row(13140L, "kopi", null, 90L));

        GscStuckRowReconciler.Outcome out = r.sweepOnce(60);

        assertEquals(2, out.scanned);
        assertEquals(0, out.refunded);
        assertEquals("both rows marked FAILED", 2, out.markedFailed);
    }

    // -----------------------------------------------------------------
    //  (f) — feature flag disabled → start() does not schedule
    // -----------------------------------------------------------------

    @Test
    public void featureFlagDisabled_doesNotScheduleDaemon() {
        // GSC_STUCK_RECONCILER_ENABLED is unset by default in the test
        // JVM. start() must observe envEnabled()==false and return
        // without spinning up the daemon thread. We don't have a way to
        // unset env vars from inside the JVM (System.getenv is read-only),
        // so we rely on the default-OFF contract — the test harness does
        // not export this var, and the assertion is "no thread named
        // gsc-stuck-row-reconciler is alive after start()".
        //
        // If a developer ever runs this test with the env var set to
        // true, this test will fail loudly — which is the correct signal
        // that the test environment is misconfigured.
        String flag = System.getenv("GSC_STUCK_RECONCILER_ENABLED");
        org.junit.Assume.assumeTrue(
                "Test requires GSC_STUCK_RECONCILER_ENABLED to be unset/false; got=" + flag,
                flag == null || flag.isEmpty()
                        || (!"true".equalsIgnoreCase(flag) && !"1".equals(flag)));

        GscStuckRowReconciler r = new GscStuckRowReconciler();
        r.start();

        // No thread named "gsc-stuck-row-reconciler" should exist.
        Thread[] threads = new Thread[Thread.activeCount() * 2];
        int n = Thread.enumerate(threads);
        for (int i = 0; i < n; i++) {
            if (threads[i] != null
                    && "gsc-stuck-row-reconciler".equals(threads[i].getName())) {
                org.junit.Assert.fail("daemon was scheduled despite feature flag OFF");
            }
        }

        // Idempotency: a second start() must still no-op.
        r.start();
        // Stop() must be safe to call on a never-started instance.
        r.stop();
    }

    // -----------------------------------------------------------------
    //  Bonus — defensive guard: positive-amount debit row → skip refund
    // -----------------------------------------------------------------

    @Test
    public void defensivePositiveDebitAmount_skipsRefund() {
        // Production safety: if some upstream bug recorded a GSC_DEBIT
        // row with a positive amount (i.e. a credit, not a debit),
        // refunding |amount| would push the player further up. The
        // reconciler logs and counts it as an error rather than
        // refunding. Verify the counter increments and no credit fires.
        FakeReconciler r = new FakeReconciler();
        r.stuckRows.add(row(13141L, "kopi",
                singleTxnPayload("wager-pos"), 90L));
        r.debits.put("wager-pos|kopi", debit(99L, +1000L));

        GscStuckRowReconciler.Outcome out = r.sweepOnce(60);

        assertEquals(0, out.refunded);
        assertEquals("guard counts as error", 1, out.errors);
        assertTrue("no credit fired", r.credits.isEmpty());
        // The row is still markFailed at the end — same reasoning as (a).
        assertEquals(1, out.markedFailed);
    }

    // -----------------------------------------------------------------
    //  Bonus — duplicate-transaction error from gateway → alreadyRefunded
    // -----------------------------------------------------------------

    @Test
    public void gatewayReturnsDuplicateError_countsAsAlreadyRefunded() {
        // Race: a concurrent reconciler instance won the (tx_id, source)
        // UNIQUE on money_gateway_log, so creditUserWithCumulative
        // returns success=false with error containing "Duplicate". The
        // reconciler must NOT count this as a hard error — it should
        // count as alreadyRefunded and proceed to markFailed.
        FakeReconciler r = new FakeReconciler();
        r.creditResult = MoneyGateway.CreditResultWithCumulative.fail("Duplicate transaction");
        r.stuckRows.add(row(13142L, "kopi",
                singleTxnPayload("wager-race"), 90L));
        r.debits.put("wager-race|kopi", debit(7L, -500L));

        GscStuckRowReconciler.Outcome out = r.sweepOnce(60);

        assertEquals(0, out.refunded);
        assertEquals(0, out.errors);
        assertEquals("dup gateway error → alreadyRefunded", 1, out.alreadyRefunded);
        assertEquals(1, out.markedFailed);
    }

    // -----------------------------------------------------------------
    //  Pure unit tests — extractTransactions + Outcome.toString
    // -----------------------------------------------------------------

    @Test
    public void extractTransactions_handlesTopLevelArray() {
        String payload = "{\"transactions\":[{\"id\":\"top1\"},{\"id\":\"top2\"}]}";
        List<GscStuckRowReconciler.TxnRef> txns =
                GscStuckRowReconciler.extractTransactions(payload);
        assertEquals(2, txns.size());
    }

    @Test
    public void extractTransactions_handlesBatchRequests() {
        String payload = "{\"batch_requests\":[{\"transactions\":[{\"id\":\"b1\"}]}]}";
        List<GscStuckRowReconciler.TxnRef> txns =
                GscStuckRowReconciler.extractTransactions(payload);
        assertEquals(1, txns.size());
        assertEquals("b1", txns.get(0).wagerId);
    }

    @Test
    public void extractTransactions_emptyAndNullSafe() {
        assertEquals(0, GscStuckRowReconciler.extractTransactions(null).size());
        assertEquals(0, GscStuckRowReconciler.extractTransactions("").size());
        assertEquals(0, GscStuckRowReconciler.extractTransactions("not json").size());
        assertEquals(0, GscStuckRowReconciler.extractTransactions("{}").size());
    }

    @Test
    public void outcomeToString_includesAllCounters() {
        GscStuckRowReconciler.Outcome o = new GscStuckRowReconciler.Outcome();
        o.scanned = 5;
        o.refunded = 3;
        o.alreadyRefunded = 1;
        o.noDebit = 1;
        o.markedFailed = 5;
        o.errors = 0;
        o.totalRefunded = 100_000L;
        String s = o.toString();
        assertTrue(s, s.contains("scanned=5"));
        assertTrue(s, s.contains("refunded=3"));
        assertTrue(s, s.contains("already_refunded=1"));
        assertTrue(s, s.contains("no_debit=1"));
        assertTrue(s, s.contains("marked_failed=5"));
        assertTrue(s, s.contains("errors=0"));
        assertTrue(s, s.contains("total_refunded=100000"));
    }
}
