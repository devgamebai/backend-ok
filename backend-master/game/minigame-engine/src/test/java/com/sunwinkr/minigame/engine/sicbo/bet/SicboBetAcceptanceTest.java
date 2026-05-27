package com.sunwinkr.minigame.engine.sicbo.bet;

import com.sunwinkr.minigame.engine.port.BetRecorder;
import com.sunwinkr.minigame.engine.port.MoneyResult;
import com.sunwinkr.minigame.engine.port.TransKind;
import com.sunwinkr.minigame.engine.port.WalletPort;
import com.sunwinkr.minigame.engine.sicbo.core.SicboRound;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 6 acceptance tests covering the SicboBetService.accept() happy and error paths.
 *
 * Tests:
 *   belowMinReturns4         — INV-13
 *   multiSidePerUserAllowed  — AMBIGUOUS #3 preserved (multi-side accepted)
 *   invalidBetTypeReturns6   — AMBIGUOUS #6 (explicit code 6 vs legacy NPE)
 *   bettingClosedReturns2    — pre-check gate
 *   botAlwaysDebits          — INV-22 / SUN-880 (distinct from TaiXiu)
 *   raceRefundsCorrectly     — race re-check after debit
 */
class SicboBetAcceptanceTest {

    private SicboBetService service;
    private SicboPotState pot;
    private SicboTxIdGenerator txGen;
    private BetRecorder recorder;

    @BeforeEach
    void setUp() {
        service  = new SicboBetService();
        pot      = new SicboPotState();
        txGen    = new SicboTxIdGenerator(1001L);
        recorder = new BetRecorder() {
            @Override
            public void record(long refId, String nickname, long betValue,
                                int inputTime, int betSideId, int moneyType) {
                /* no-op */
            }
        };
    }

    // -----------------------------------------------------------------------
    // INV-13: minimum bet
    // -----------------------------------------------------------------------

    /** bet=50 is below MIN_BET=100 → error code 4. */
    @Test
    void belowMinReturns4() {
        SicboRound round = new SicboRound(1001L); // OPEN phase

        WalletPort wallet = new StubWallet(500_000L, true);
        SicboBetRequest req = req("player1", 50L, "TAI", false);

        SicboBetAcceptResult result = service.accept(req, round, txGen, pot, wallet, recorder);

        assertThat(result.errorCode).isEqualTo(4);
        assertThat(result.isSuccess()).isFalse();
        assertThat(pot.size()).isEqualTo(0); // no entry added
    }

    // -----------------------------------------------------------------------
    // AMBIGUOUS #3: multi-side per user is allowed
    // -----------------------------------------------------------------------

    /**
     * A user bets TAI then XIU in the same round. Both must be accepted
     * (PRESERVED-DEAD-CODE cross-side guard never fires for PotSicbo IDs).
     */
    @Test
    void multiSidePerUserAllowed() {
        SicboRound round = new SicboRound(1001L);
        WalletPort wallet = new StubWallet(1_000_000L, true);

        SicboBetRequest betTai = req("player1", 1000L, "TAI", false);
        SicboBetRequest betXiu = req("player1", 1000L, "XIU", false);

        SicboBetAcceptResult r1 = service.accept(betTai, round, txGen, pot, wallet, recorder);
        SicboBetAcceptResult r2 = service.accept(betXiu, round, txGen, pot, wallet, recorder);

        assertThat(r1.errorCode).as("TAI bet error code").isEqualTo(0);
        assertThat(r2.errorCode).as("XIU bet error code").isEqualTo(0);
        assertThat(pot.size()).isEqualTo(2);

        // Verify the two entries have different betSideIds (TAI=48, XIU=49)
        assertThat(pot.listUserBet().get(0).betSideId).isEqualTo(SicboBetType.TAI.getId());
        assertThat(pot.listUserBet().get(1).betSideId).isEqualTo(SicboBetType.XIU.getId());
    }

    // -----------------------------------------------------------------------
    // AMBIGUOUS #6: invalid bet type name → code 6
    // -----------------------------------------------------------------------

    /** Unknown betSideName "GARBAGE" → code 6 (replaces legacy NPE). */
    @Test
    void invalidBetTypeReturns6() {
        SicboRound round = new SicboRound(1001L);
        WalletPort wallet = new StubWallet(500_000L, true);
        SicboBetRequest req = req("player1", 1000L, "GARBAGE", false);

        SicboBetAcceptResult result = service.accept(req, round, txGen, pot, wallet, recorder);

        assertThat(result.errorCode).isEqualTo(6);
        assertThat(pot.size()).isEqualTo(0);
    }

    // -----------------------------------------------------------------------
    // Betting closed pre-check
    // -----------------------------------------------------------------------

    /** round.lockBetting() called first → pre-check returns code 2. */
    @Test
    void bettingClosedReturns2() {
        SicboRound round = new SicboRound(1001L);
        round.lockBetting(); // OPEN → LOCKED

        WalletPort wallet = new StubWallet(500_000L, true);
        SicboBetRequest req = req("player1", 1000L, "TAI", false);

        SicboBetAcceptResult result = service.accept(req, round, txGen, pot, wallet, recorder);

        assertThat(result.errorCode).isEqualTo(2);
        assertThat(pot.size()).isEqualTo(0);
    }

    // -----------------------------------------------------------------------
    // INV-22 / SUN-880: bots always debit (distinct from TaiXiu)
    // -----------------------------------------------------------------------

    /**
     * isBot=true → WalletPort.debit IS called.
     * This is the Sicbo-specific behaviour introduced in SUN-880 (AMBIGUOUS #6 /
     * INV-22 inverse): bots in Sicbo debit real money, unlike TaiXiu bots which
     * used virtual-only debits before SUN-880.
     */
    @Test
    void botAlwaysDebits() {
        SicboRound round = new SicboRound(1001L);
        TrackingWallet wallet = new TrackingWallet(1_000_000L);

        SicboBetRequest req = req("bot_player", 5000L, "TAI", /* isBot= */ true);

        SicboBetAcceptResult result = service.accept(req, round, txGen, pot, wallet, recorder);

        assertThat(result.isSuccess()).isTrue();
        assertThat(wallet.debitCalled).isTrue();   // debit WAS called for bot
        assertThat(pot.size()).isEqualTo(1);
        // Bot bets do NOT count toward totalValueBetUser (RTP balancer input)
        assertThat(pot.totalValueBetUser()).isEqualTo(0L);
    }

    // -----------------------------------------------------------------------
    // Race re-check: betting closes between debit and pot-add
    // -----------------------------------------------------------------------

    /**
     * The round closes between the pre-check and pot-add (simulated by a
     * WalletPort impl that locks betting during debit). Service must
     * auto-refund and return code 1.
     */
    @Test
    void raceRefundsCorrectly() {
        SicboRound round = new SicboRound(1001L);
        // Wallet that locks the round during debit to simulate the race
        WalletPort wallet = new RaceWallet(round, 1_000_000L);

        SicboBetRequest req = req("player1", 1000L, "TAI", false);
        TrackingBetRecorder rec = new TrackingBetRecorder();

        SicboBetAcceptResult result = service.accept(req, round, txGen, pot, wallet, rec);

        assertThat(result.errorCode).isEqualTo(1);
        assertThat(pot.size()).isEqualTo(0);                // bet NOT in pot
        assertThat(((RaceWallet) wallet).creditCalled).isTrue(); // refund issued
        assertThat(rec.recorded).isFalse();                 // recorder NOT called
    }

    // -----------------------------------------------------------------------
    // Helpers
    // -----------------------------------------------------------------------

    private static SicboBetRequest req(String nickname, long betValue, String side, boolean isBot) {
        return new SicboBetRequest(nickname, 42, betValue, (short) 10, (short) 1, side, isBot);
    }

    /** Wallet stub: configurable balance + always-success debit. */
    private static class StubWallet implements WalletPort {
        private long balance;
        private final boolean debitSuccess;

        StubWallet(long balance, boolean debitSuccess) {
            this.balance = balance;
            this.debitSuccess = debitSuccess;
        }

        @Override public long getBalance(String n, String t) { return balance; }

        @Override public MoneyResult debit(String n, long amt, String t, long txId, TransKind k) {
            if (!debitSuccess) return MoneyResult.fail(1);
            if (amt > balance)  return MoneyResult.fail(3);
            balance -= amt;
            return MoneyResult.ok(balance);
        }

        @Override public MoneyResult credit(String n, long amt, String t, long txId, TransKind k) {
            balance += amt;
            return MoneyResult.ok(balance);
        }
    }

    /** Wallet that records whether debit was called. */
    private static class TrackingWallet extends StubWallet {
        boolean debitCalled = false;
        TrackingWallet(long balance) { super(balance, true); }

        @Override public MoneyResult debit(String n, long amt, String t, long txId, TransKind k) {
            debitCalled = true;
            return super.debit(n, amt, t, txId, k);
        }
    }

    /**
     * Wallet that simulates the race condition: locks the round during debit
     * so the post-debit re-check sees isBetting()==false.
     */
    private static class RaceWallet implements WalletPort {
        private final SicboRound round;
        private long balance;
        boolean creditCalled = false;

        RaceWallet(SicboRound round, long balance) {
            this.round   = round;
            this.balance = balance;
        }

        @Override public long getBalance(String n, String t) { return balance; }

        @Override public MoneyResult debit(String n, long amt, String t, long txId, TransKind k) {
            balance -= amt;
            round.lockBetting(); // <-- simulate race: round closes mid-debit
            return MoneyResult.ok(balance);
        }

        @Override public MoneyResult credit(String n, long amt, String t, long txId, TransKind k) {
            creditCalled = true;
            balance += amt;
            return MoneyResult.ok(balance);
        }
    }

    /** BetRecorder that tracks whether record() was called. */
    private static class TrackingBetRecorder implements BetRecorder {
        boolean recorded = false;
        @Override public void record(long refId, String nick, long betVal,
                                     int inputTime, int betSideId, int moneyType) {
            recorded = true;
        }
    }
}
