package com.sunwinkr.lottery.engine.ingest;

import com.sunwinkr.lottery.engine.model.LotteryResult;
import com.sunwinkr.lottery.engine.model.LotteryTicket;
import com.sunwinkr.lottery.engine.port.BetStore;
import com.sunwinkr.lottery.engine.port.MoneyResult;
import com.sunwinkr.lottery.engine.port.ResultStore;
import com.sunwinkr.lottery.engine.port.ScrapeClient;
import com.sunwinkr.lottery.engine.port.SettledFlagStore;
import com.sunwinkr.lottery.engine.port.TransKind;
import com.sunwinkr.lottery.engine.port.WalletPort;
import com.sunwinkr.lottery.engine.settle.LotterySettleService;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Orchestration tests — DrawIngest dedupe + crash-resume semantics.
 *
 * <p>Covers I3 (dedupe), I6 (crash-resumable) from
 * {@code docs/plans/lottery-extraction-plan.md §2.2}.
 */
class DrawIngestTest {

    private final LocalDate vnDate = LocalDate.of(2026, 5, 14);

    @Test
    void dedupeOnSameDate_secondCallNoOps() {
        FakeScrape scrape = new FakeScrape(simpleDraw());
        FakeResultStore rs = new FakeResultStore();
        FakeSettled settled = new FakeSettled();
        FakeBetStore bs = new FakeBetStore();
        LotterySettleService svc = new LotterySettleService(bs, new FakeWallet(), null);
        DrawIngest ingest = new DrawIngest(scrape, rs, settled, svc, draw -> "fake-json");

        // First call — scrape, save, settle, mark.
        DrawIngest.IngestSummary first = ingest.runOnce(vnDate);
        assertThat(first.newScrapePersisted).isTrue();
        assertThat(scrape.calls).isEqualTo(1);

        // Second call same date — should noop because settled flag set.
        DrawIngest.IngestSummary second = ingest.runOnce(vnDate);
        assertThat(second.newScrapePersisted).isFalse();
        assertThat(scrape.calls).isEqualTo(1); // NOT re-scraped
    }

    @Test
    void crashMidSettleResumes_secondCallSkipsScrapeButReruns() {
        FakeScrape scrape = new FakeScrape(simpleDraw());
        FakeResultStore rs = new FakeResultStore();
        FakeSettled settled = new FakeSettled();
        // Simulate "crash" by failing the markSettled on first call.
        settled.failNextMark = true;
        FakeBetStore bs = new FakeBetStore();
        LotterySettleService svc = new LotterySettleService(bs, new FakeWallet(), null);
        DrawIngest ingest = new DrawIngest(scrape, rs, settled, svc, draw -> "fake-json");

        try {
            ingest.runOnce(vnDate);
        } catch (RuntimeException ignored) {
            // expected — the markSettled crashed
        }
        assertThat(scrape.calls).isEqualTo(1);
        assertThat(rs.saves).hasSize(1);
        assertThat(settled.marked).doesNotContain(vnDate);

        // Resume — should skip scrape (row already there) but re-run settle + mark.
        DrawIngest.IngestSummary second = ingest.runOnce(vnDate);
        assertThat(scrape.calls).isEqualTo(1); // NOT re-scraped
        assertThat(second.newScrapePersisted).isFalse();
        assertThat(settled.marked).contains(vnDate);
    }

    // ---------- Helpers / fakes ----------

    private static LotteryResult simpleDraw() {
        LotteryResult rs = new LotteryResult();
        LotteryResult.Results r = new LotteryResult.Results();
        r.setĐB(Collections.singletonList("12345"));
        r.setG1(Collections.<String>emptyList());
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

    static final class FakeScrape implements ScrapeClient {
        final LotteryResult payload;
        int calls;

        FakeScrape(LotteryResult payload) {
            this.payload = payload;
        }

        @Override
        public LotteryResult fetch() {
            calls++;
            return payload;
        }
    }

    static final class FakeResultStore implements ResultStore {
        final Map<LocalDate, LotteryResult> rows = new HashMap<>();
        final List<String> saves = new ArrayList<>();

        @Override
        public Optional<LotteryResult> findByDate(LocalDate vnDate) {
            return Optional.ofNullable(rows.get(vnDate));
        }

        @Override
        public void save(String rawJson, LocalDate vnDate) {
            saves.add(rawJson);
            LotteryResult rs = new LotteryResult();
            LotteryResult.Results r = new LotteryResult.Results();
            r.setĐB(Collections.singletonList("12345"));
            r.setG1(Collections.<String>emptyList());
            r.setG2(Collections.<String>emptyList());
            r.setG3(Collections.<String>emptyList());
            r.setG4(Collections.<String>emptyList());
            r.setG5(Collections.<String>emptyList());
            r.setG6(Collections.<String>emptyList());
            r.setG7(Collections.<String>emptyList());
            rs.setResults(r);
            rs.setTime("14-05-2026");
            rows.put(vnDate, rs);
        }

        @Override
        public List<LotteryResult> listSettled(LocalDate from, LocalDate to) {
            return new ArrayList<>(rows.values());
        }
    }

    static final class FakeSettled implements SettledFlagStore {
        final List<LocalDate> marked = new ArrayList<>();
        boolean failNextMark = false;

        @Override
        public boolean isSettled(LocalDate vnDate) {
            return marked.contains(vnDate);
        }

        @Override
        public void markSettled(LocalDate vnDate) {
            if (failNextMark) {
                failNextMark = false;
                throw new RuntimeException("simulated crash");
            }
            marked.add(vnDate);
        }
    }

    static final class FakeBetStore implements BetStore {
        @Override public LotteryTicket insert(LotteryTicket t) { return t; }
        @Override public List<LotteryTicket> findPendingForDate(LocalDate d, LocalTime t) { return Collections.emptyList(); }
        @Override public void markSettled(long ticketId, long prize) { }
        @Override public int voidTicket(long ticketId) { return 0; }
        @Override public java.util.Optional<com.sunwinkr.lottery.engine.model.LotteryTicket> findById(long ticketId) { return java.util.Optional.empty(); }
        @Override public List<LotteryTicket> findByUser(String n, Paging p) { return Collections.emptyList(); }
        @Override public List<LotteryTicket> search(SearchFilter f) { return Collections.emptyList(); }
        @Override public long count(SearchFilter f) { return 0; }
    }

    static final class FakeWallet implements WalletPort {
        @Override
        public MoneyResult debit(String n, long a, String mt, String s, String g,
                                 String d, long fee, long tx, TransKind k) {
            return MoneyResult.ok(0);
        }
        @Override
        public MoneyResult credit(String n, long a, String mt, String s, String g,
                                  String d, long fee, long tx, TransKind k) {
            return MoneyResult.ok(0);
        }
    }
}
