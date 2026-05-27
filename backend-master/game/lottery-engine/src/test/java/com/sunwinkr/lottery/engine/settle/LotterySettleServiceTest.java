package com.sunwinkr.lottery.engine.settle;

import com.sunwinkr.lottery.engine.model.LotteryResult;
import com.sunwinkr.lottery.engine.model.LotteryTicket;
import com.sunwinkr.lottery.engine.model.SettleStatus;
import com.sunwinkr.lottery.engine.port.BetStore;
import com.sunwinkr.lottery.engine.port.MoneyResult;
import com.sunwinkr.lottery.engine.port.TransKind;
import com.sunwinkr.lottery.engine.port.WalletPort;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Settle-loop tests — per-row resilience + ordering.
 *
 * <p>Covers S1 (per-row try/catch) and S5 (markDaySettled ordering) from
 * {@code docs/plans/lottery-extraction-plan.md §2.5}.
 */
class LotterySettleServiceTest {

    @Test
    void perRowFailureContinues() {
        // 3 winning tickets. The 2nd wallet credit fails. Expectation:
        // settle loop processes 1st + 3rd, emits 1 failure for 2nd.
        FakeBetStore betStore = new FakeBetStore(Arrays.asList(
                winningTicket(1L, "userA"),
                winningTicket(2L, "userB"),
                winningTicket(3L, "userC")));
        FakeWallet wallet = new FakeWallet();
        wallet.failOn = "userB";
        List<SettleFailureEvent> failures = new ArrayList<>();

        LotterySettleService svc = new LotterySettleService(betStore, wallet, failures::add);
        SettleSummary summary = svc.settleAll(LocalDate.of(2026, 5, 14), drawMatching42());

        assertThat(summary.getSettledCount()).isEqualTo(2);
        assertThat(summary.getFailureCount()).isEqualTo(1);
        assertThat(failures).hasSize(1);
        assertThat(failures.get(0).getNickname()).isEqualTo("userB");
        // 2 markSettled calls — failed row stays pending.
        assertThat(betStore.marked).containsExactly(1L, 3L);
    }

    @Test
    void markDaySettledIsLastWrite() {
        // The day-level markSettled is owned by DrawIngest, not the
        // settle service. We assert the settle service does NOT itself
        // touch a day-level flag — see DrawIngestTest for the ordering.
        FakeBetStore betStore = new FakeBetStore(Collections.singletonList(
                winningTicket(1L, "userA")));
        FakeWallet wallet = new FakeWallet();
        AtomicInteger order = new AtomicInteger(0);
        int[] perRowMarkOrder = new int[]{-1};
        betStore.markCallback = () -> perRowMarkOrder[0] = order.incrementAndGet();

        LotterySettleService svc = new LotterySettleService(betStore, wallet, null);
        svc.settleAll(LocalDate.of(2026, 5, 14), drawMatching42());

        // markSettled was called exactly once on the per-row store.
        // The day-level flag flip ordering is the DrawIngest test.
        assertThat(perRowMarkOrder[0]).isEqualTo(1);
        assertThat(betStore.marked).containsExactly(1L);
    }

    @Test
    void losingTicketStillMarkedSettled() {
        // Even when prize = 0, the row should be settled so it doesn't
        // bounce back into the pending pool next ingest pass.
        LotteryTicket losing = new LotteryTicket(
                7L, 100L, "loser", 220_000L, 1, "99",
                null, LocalDateTime.now(), null,
                10_000L, 22, 80);
        FakeBetStore betStore = new FakeBetStore(Collections.singletonList(losing));
        FakeWallet wallet = new FakeWallet();
        LotterySettleService svc = new LotterySettleService(betStore, wallet, null);

        SettleSummary s = svc.settleAll(LocalDate.of(2026, 5, 14), drawMatching42());

        assertThat(s.getSettledCount()).isEqualTo(1);
        assertThat(s.getFailureCount()).isZero();
        assertThat(betStore.marked).containsExactly(7L);
        assertThat(wallet.creditCalls).isZero(); // never credited
    }

    @Test
    void bet_settle_isIdempotent() {
        // A ticket already SETTLED (settle_status = SETTLED) must be skipped
        // by the settle loop — no second wallet credit, no second markSettled.
        LotteryTicket alreadySettled = new LotteryTicket(
                5L, 200L, "userD", 220_000L, 1, "42",
                176_000L, LocalDateTime.now(), LocalDateTime.now(),
                10_000L, 22, 80,
                SettleStatus.SETTLED);
        FakeBetStore betStore = new FakeBetStore(Collections.singletonList(alreadySettled));
        FakeWallet wallet = new FakeWallet();
        LotterySettleService svc = new LotterySettleService(betStore, wallet, null);

        SettleSummary s = svc.settleAll(LocalDate.of(2026, 5, 14), drawMatching42());

        // Already-settled ticket counts as settled in summary.
        assertThat(s.getSettledCount()).isEqualTo(1);
        assertThat(s.getFailureCount()).isZero();
        // markSettled must NOT be called again — row already stamped.
        assertThat(betStore.marked).isEmpty();
        // No wallet credit issued.
        assertThat(wallet.creditCalls).isZero();
    }

    // ---------- Fixtures ----------

    private static LotteryResult drawMatching42() {
        LotteryResult rs = new LotteryResult();
        LotteryResult.Results r = new LotteryResult.Results();
        r.setĐB(Collections.singletonList("12342"));
        r.setG1(Collections.singletonList("99942"));
        r.setG2(Collections.<String>emptyList());
        r.setG3(Collections.<String>emptyList());
        r.setG4(Collections.<String>emptyList());
        r.setG5(Collections.<String>emptyList());
        r.setG6(Collections.<String>emptyList());
        r.setG7(Collections.<String>emptyList());
        rs.setResults(r);
        rs.setTime("14-05-2026");
        return rs;
    }

    private static LotteryTicket winningTicket(long id, String user) {
        return new LotteryTicket(
                id, 100L + id, user, 220_000L, 1, "42",
                null, LocalDateTime.now(), null,
                10_000L, 22, 80);
    }

    // ---------- Fakes ----------

    static final class FakeBetStore implements BetStore {
        final List<LotteryTicket> pending;
        final List<Long> marked = new ArrayList<>();
        Runnable markCallback;

        FakeBetStore(List<LotteryTicket> pending) {
            this.pending = pending;
        }

        @Override public LotteryTicket insert(LotteryTicket t) { return t; }
        @Override public List<LotteryTicket> findPendingForDate(LocalDate d, LocalTime t) { return pending; }

        @Override
        public void markSettled(long ticketId, long prize) {
            marked.add(ticketId);
            if (markCallback != null) markCallback.run();
        }

        @Override public Optional<LotteryTicket> findById(long ticketId) { return Optional.empty(); }
        @Override public int voidTicket(long ticketId) { return 1; }
        @Override public List<LotteryTicket> findByUser(String n, Paging p) { return Collections.emptyList(); }
        @Override public List<LotteryTicket> search(SearchFilter f) { return Collections.emptyList(); }
        @Override public long count(SearchFilter f) { return 0; }
    }

    static final class FakeWallet implements WalletPort {
        String failOn; // nickname to fail
        int creditCalls;

        @Override
        public MoneyResult debit(String n, long a, String mt, String s, String g,
                                 String d, long fee, long tx, TransKind k) {
            return MoneyResult.ok(0);
        }

        @Override
        public MoneyResult credit(String n, long a, String mt, String s, String g,
                                  String d, long fee, long tx, TransKind k) {
            creditCalls++;
            if (failOn != null && failOn.equals(n)) return MoneyResult.fail("9999");
            return MoneyResult.ok(0);
        }
    }
}
