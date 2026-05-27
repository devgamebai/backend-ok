package com.sunwinkr.minigame.engine.bet;

import com.sunwinkr.minigame.engine.core.RevealClock;
import com.sunwinkr.minigame.engine.core.TaiXiuRound;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Plan §2.2 row B1 — invariants on the bet-acceptance error-code path.
 * Covers spec invariants:
 * <ul>
 *   <li>INV-4 — cross-side bet block</li>
 *   <li>INV-13 — bet min (MIN_BET = 100)</li>
 *   <li>INV-22 — livestream isolation (TaiXiu bots SKIP wallet debit;
 *       livestream + real DO debit)</li>
 * </ul>
 */
class BetAcceptanceInvariantTest {

    private static TaiXiuRound openRound() {
        return new TaiXiuRound(new RevealClock.SimpleRevealClock(), new NoOpCachePort());
    }

    private static BetRequest realBet(String user, long value, short side) {
        return new BetRequest(user, /*userId*/ 42, value,
            /*inputTime*/ (short) 30,
            /*moneyType*/ (short) 1,
            side,
            /*isBot*/ false,
            /*isLivestream*/ false);
    }

    @Test
    void errorCodeOrdering() {
        // Priority per BetAcceptor javadoc: 2 → 4 → 3 → 5 → wallet → race.
        // Construct a single bet that violates 4 AND 3 AND 5 and assert
        // the lowest-priority violation wins (4).
        BetAcceptor acceptor = new BetAcceptor();
        BetLedger ledger = new BetLedger();
        InMemoryWalletPort wallet = new InMemoryWalletPort().seed("u", 5L);
        TaiXiuRound round = openRound();

        // Pre-seed: u has bet Tài → if u now bets Xỉu that would trigger
        // code 5. Combined with value below min (4) AND above balance (3).
        long preBetValue = 200L;
        wallet.seed("u", preBetValue + 5L);
        BetAcceptResult preBet = acceptor.accept(
            realBet("u", preBetValue, (short) 1), round, ledger, wallet, null);
        assertThat(preBet.isOk()).as("setup bet should succeed").isTrue();

        // Now: value=50 (below min=100), balance after preBet=5 → also
        // insufficient for any value > 5, and side=0 → cross-side.
        BetAcceptResult res = acceptor.accept(
            realBet("u", 50L, (short) 0), round, ledger, wallet, null);
        assertThat(res.errorCode)
            .as("priority: 4 (below min) beats 3 and 5")
            .isEqualTo(4);

        // Bumping above min still hits 3 (insufficient) before 5.
        BetAcceptResult res2 = acceptor.accept(
            realBet("u", 1_000_000L, (short) 0), round, ledger, wallet, null);
        assertThat(res2.errorCode)
            .as("priority: 3 (insufficient) beats 5")
            .isEqualTo(3);

        // Top up wallet → now 5 triggers.
        wallet.seed("u", 1_000_000L);
        BetAcceptResult res3 = acceptor.accept(
            realBet("u", 500L, (short) 0), round, ledger, wallet, null);
        assertThat(res3.errorCode)
            .as("cross-side now the only violation")
            .isEqualTo(5);
    }

    @Test
    void belowMinReturns4() {
        // INV-13: every accepted bet >= MIN_BET (100). A bet of 50 → code 4.
        BetAcceptor acceptor = new BetAcceptor();
        BetLedger ledger = new BetLedger();
        InMemoryWalletPort wallet = new InMemoryWalletPort().seed("u", 1_000_000L);
        TaiXiuRound round = openRound();

        BetAcceptResult res = acceptor.accept(
            realBet("u", 50L, (short) 1), round, ledger, wallet, null);

        assertThat(res.errorCode).isEqualTo(4);
        assertThat(wallet.debitCount()).as("no wallet debit on code 4").isZero();
        assertThat(ledger.potTai().totalValue()).as("no pot append on code 4").isZero();
    }

    @Test
    void crossSideReturns5() {
        // INV-4: same user cannot bet Tài AND Xỉu in same round.
        BetAcceptor acceptor = new BetAcceptor();
        BetLedger ledger = new BetLedger();
        InMemoryWalletPort wallet = new InMemoryWalletPort().seed("u", 10_000L);
        TaiXiuRound round = openRound();

        BetAcceptResult tai = acceptor.accept(
            realBet("u", 500L, (short) 1), round, ledger, wallet, null);
        assertThat(tai.isOk()).isTrue();
        assertThat(ledger.potTai().totalValue()).isEqualTo(500L);

        BetAcceptResult xiu = acceptor.accept(
            realBet("u", 500L, (short) 0), round, ledger, wallet, null);
        assertThat(xiu.errorCode).isEqualTo(5);
        assertThat(ledger.potXiu().totalValue())
            .as("no pot mutation on cross-side reject")
            .isZero();
        assertThat(wallet.debitCount())
            .as("only the first bet debits the wallet")
            .isEqualTo(1);
    }

    @Test
    void botWalletSkipped() {
        // INV-22: pure bot (isBot=true, isLivestream=false) → no wallet debit.
        // Legacy code (TXR:408) still applies the balance check before the
        // bot-wallet-skip branch, so seed enough balance to clear it; the
        // assertion is that the wallet adapter is never invoked for the
        // debit itself.
        BetAcceptor acceptor = new BetAcceptor();
        BetLedger ledger = new BetLedger();
        InMemoryWalletPort wallet = new InMemoryWalletPort().seed("bot1", 10_000L);
        TaiXiuRound round = openRound();

        BetRequest bot = new BetRequest("bot1", 0, 500L,
            (short) 30, (short) 1, (short) 1,
            /*isBot*/ true, /*isLivestream*/ false);

        BetAcceptResult res = acceptor.accept(bot, round, ledger, wallet, null);
        assertThat(res.isOk()).isTrue();
        assertThat(wallet.debitCount())
            .as("bot path skips wallet debit (TXR:431)")
            .isZero();
        assertThat(ledger.potTai().totalValue())
            .as("pot still mutated for bots")
            .isEqualTo(500L);
        assertThat(ledger.potTai().botStats().numBot)
            .as("bot routed through addBot")
            .isEqualTo(1);
    }

    @Test
    void livestreamBotDoesDebit() {
        // INV-22 corollary: livestream bots DO hit the wallet (TXR:431-435).
        BetAcceptor acceptor = new BetAcceptor();
        BetLedger ledger = new BetLedger();
        InMemoryWalletPort wallet = new InMemoryWalletPort().seed("livebot", 10_000L);
        TaiXiuRound round = openRound();

        BetRequest live = new BetRequest("livebot", 0, 500L,
            (short) 30, (short) 1, (short) 1,
            /*isBot*/ true, /*isLivestream*/ true);

        BetAcceptResult res = acceptor.accept(live, round, ledger, wallet, null);
        assertThat(res.isOk()).isTrue();
        assertThat(wallet.debitCount())
            .as("livestream bot debits real wallet")
            .isEqualTo(1);
    }
}
