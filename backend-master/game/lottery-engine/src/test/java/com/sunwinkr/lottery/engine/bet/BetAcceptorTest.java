package com.sunwinkr.lottery.engine.bet;

import com.sunwinkr.lottery.engine.clock.LotteryClock;
import com.sunwinkr.lottery.engine.model.LotteryTicket;
import com.sunwinkr.lottery.engine.port.BetStore;
import com.sunwinkr.lottery.engine.port.MoneyResult;
import com.sunwinkr.lottery.engine.port.SettledFlagStore;
import com.sunwinkr.lottery.engine.port.TransKind;
import com.sunwinkr.lottery.engine.port.WalletPort;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneOffset;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * End-to-end orchestration tests for {@link BetAcceptor} — wire fake
 * ports together and assert the {@link BetAcceptResult} code.
 *
 * <p>Validates the order documented in
 * {@code docs/plans/lottery-extraction-plan.md §2.3 B1}: validation →
 * lock → snapshot → debit → insert.
 */
class BetAcceptorTest {

    private static Clock vnClock(int hour, int min) {
        Instant instant = LocalDateTime.of(LocalDate.of(2026, 5, 14), LocalTime.of(hour, min))
                .atZone(LotteryClock.VN).toInstant();
        return Clock.fixed(instant, ZoneOffset.UTC);
    }

    @Test
    void errorCodeOrdering_invalidShapeReturns0005BeforeWalletCalled() {
        FakeWallet wallet = new FakeWallet(MoneyResult.ok(1_000_000L));
        FakeBetStore betStore = new FakeBetStore();
        FakeSettledFlag flag = new FakeSettledFlag(false);
        BetAcceptor acceptor = new BetAcceptor(wallet, betStore, flag, vnClock(10, 0));

        // mode 1 wants 2-digit; "1,2" is csv → invalidNumber.
        BetAcceptResult r = acceptor.accept(
                new BetRequest("u", 1L, 1, "1,2", 10_000L, "n"));

        assertThat(r.getErrorCode()).isEqualTo("0005");
        assertThat(wallet.debitCalls).isZero();
        assertThat(betStore.inserts).isEmpty();
    }

    @Test
    void errorCodeOrdering_walletFailureReturns0001() {
        FakeWallet wallet = new FakeWallet(MoneyResult.fail("9999"));
        FakeBetStore betStore = new FakeBetStore();
        FakeSettledFlag flag = new FakeSettledFlag(false);
        BetAcceptor acceptor = new BetAcceptor(wallet, betStore, flag, vnClock(10, 0));

        BetAcceptResult r = acceptor.accept(
                new BetRequest("u", 1L, 1, "42", 10_000L, "n"));

        assertThat(r.getErrorCode()).isEqualTo("0001");
        assertThat(wallet.debitCalls).isEqualTo(1);
        assertThat(betStore.inserts).isEmpty();
    }

    @Test
    void errorCodeOrdering_insufficientReturns0003() {
        FakeWallet wallet = new FakeWallet(MoneyResult.fail("0003"));
        FakeBetStore betStore = new FakeBetStore();
        FakeSettledFlag flag = new FakeSettledFlag(false);
        BetAcceptor acceptor = new BetAcceptor(wallet, betStore, flag, vnClock(10, 0));

        BetAcceptResult r = acceptor.accept(
                new BetRequest("u", 1L, 1, "42", 10_000L, "n"));

        assertThat(r.getErrorCode()).isEqualTo("0003");
    }

    @Test
    void locked_18_30_VN_returns0002() {
        FakeWallet wallet = new FakeWallet(MoneyResult.ok(1_000_000L));
        FakeBetStore betStore = new FakeBetStore();
        FakeSettledFlag flag = new FakeSettledFlag(false);
        BetAcceptor acceptor = new BetAcceptor(wallet, betStore, flag, vnClock(18, 30));

        BetAcceptResult r = acceptor.accept(
                new BetRequest("u", 1L, 1, "42", 10_000L, "n"));

        assertThat(r.getErrorCode()).isEqualTo("0002");
        assertThat(wallet.debitCalls).isZero();
    }

    @Test
    void happyPathInsertsTicketWithSnapshot() {
        FakeWallet wallet = new FakeWallet(MoneyResult.ok(800_000L));
        FakeBetStore betStore = new FakeBetStore();
        FakeSettledFlag flag = new FakeSettledFlag(false);
        BetAcceptor acceptor = new BetAcceptor(wallet, betStore, flag, vnClock(10, 0));

        BetAcceptResult r = acceptor.accept(
                new BetRequest("u", 1L, 1, "42", 10_000L, "n"));

        assertThat(r.isSuccess()).isTrue();
        assertThat(r.getCurrentMoney()).isEqualTo(800_000L);
        assertThat(betStore.inserts).hasSize(1);

        LotteryTicket t = betStore.inserts.get(0);
        // SUN-1295: snapshot stamped at purchase = mode 1 (rate=22, prizeMul=80).
        assertThat(t.getRateAtPurchase()).isEqualTo(22);
        assertThat(t.getPrizeMultiplier()).isEqualTo(80);
        // betValue = userBet * rate = 10000 * 22 = 220_000
        assertThat(t.getBetValue()).isEqualTo(220_000L);
        assertThat(t.getBetUnit()).isEqualTo(10_000L);
        // Wallet was debited the rate-multiplied amount.
        assertThat(wallet.lastDebitAmount).isEqualTo(220_000L);
        assertThat(wallet.lastDebitKind).isEqualTo(TransKind.START);
    }

    // ---------- Fakes ----------

    static final class FakeWallet implements WalletPort {
        final MoneyResult debitResult;
        int debitCalls;
        long lastDebitAmount;
        TransKind lastDebitKind;

        FakeWallet(MoneyResult debitResult) {
            this.debitResult = debitResult;
        }

        @Override
        public MoneyResult debit(String n, long amount, String mt, String s, String g,
                                 String d, long fee, long tx, TransKind k) {
            debitCalls++;
            lastDebitAmount = amount;
            lastDebitKind = k;
            return debitResult;
        }

        @Override
        public MoneyResult credit(String n, long amount, String mt, String s, String g,
                                  String d, long fee, long tx, TransKind k) {
            return MoneyResult.ok(0);
        }
    }

    static final class FakeBetStore implements BetStore {
        final AtomicLong idSeq = new AtomicLong(1);
        final java.util.List<LotteryTicket> inserts = new java.util.ArrayList<>();

        @Override
        public LotteryTicket insert(LotteryTicket ticket) {
            long id = idSeq.getAndIncrement();
            LotteryTicket stored = new LotteryTicket(
                    id, ticket.getUserId(), ticket.getNickname(), ticket.getBetValue(),
                    ticket.getModeId(), ticket.getTicket(), ticket.getPrize(),
                    ticket.getCreatedDate(), ticket.getSettledAt(),
                    ticket.getBetUnit(), ticket.getRateAtPurchase(), ticket.getPrizeMultiplier());
            inserts.add(stored);
            return stored;
        }

        @Override public List<LotteryTicket> findPendingForDate(LocalDate d, LocalTime t) { return Collections.emptyList(); }
        @Override public void markSettled(long ticketId, long prize) { }
        @Override public int voidTicket(long ticketId) { return 0; }
        @Override public java.util.Optional<com.sunwinkr.lottery.engine.model.LotteryTicket> findById(long ticketId) { return java.util.Optional.empty(); }
        @Override public List<LotteryTicket> findByUser(String n, Paging p) { return Collections.emptyList(); }
        @Override public List<LotteryTicket> search(SearchFilter f) { return Collections.emptyList(); }
        @Override public long count(SearchFilter f) { return 0; }
    }

    static final class FakeSettledFlag implements SettledFlagStore {
        final boolean v;
        FakeSettledFlag(boolean v) { this.v = v; }
        @Override public boolean isSettled(LocalDate d) { return v; }
        @Override public void markSettled(LocalDate d) { }
    }
}
