package com.sunwinkr.minigame.engine.bet;

import com.sunwinkr.minigame.engine.core.RevealClock;
import com.sunwinkr.minigame.engine.core.TaiXiuRound;
import com.sunwinkr.minigame.engine.port.TransKind;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Plan §2.2 row B5 — mid-debit disable refund.
 *
 * <p>Reproduces the legacy TXR:437 race: after a successful wallet
 * debit, the round transitions to LOCKED (e.g. count=45 ticked between
 * debit and post-check). The acceptor must auto-refund via
 * {@link com.sunwinkr.minigame.engine.port.WalletPort#credit} using
 * the SAME perBetTxId and return code 1.
 */
class BetRaceTest {

    /**
     * Custom {@code WalletPort} wrapper that flips the round phase
     * during the debit call, so the post-debit re-check fails.
     */
    private static final class RaceFlipWallet extends InMemoryWalletPort {
        final TaiXiuRound round;

        RaceFlipWallet(TaiXiuRound round) {
            this.round = round;
        }

        @Override
        public com.sunwinkr.minigame.engine.port.MoneyResult debit(
                String user, long amount, String moneyType,
                String source, long gameId, String desc,
                long fee, long txId, TransKind transKind) {
            com.sunwinkr.minigame.engine.port.MoneyResult res =
                super.debit(user, amount, moneyType, source, gameId, desc, fee, txId, transKind);
            // Mid-call flip: legacy disableBetting() racing with bet()
            // (TXR:251-254 vs TXR:432-437). lockBetting() is the public
            // L4 entry — it transitions OPEN → LOCKED.
            round.lockBetting();
            return res;
        }
    }

    @Test
    void midDebitDisableRefunds() {
        TaiXiuRound round = new TaiXiuRound(new RevealClock.SimpleRevealClock(), new NoOpCachePort());
        BetLedger ledger = new BetLedger();
        RaceFlipWallet wallet = new RaceFlipWallet(round);
        wallet.seed("u", 10_000L);
        BetAcceptor acceptor = new BetAcceptor();

        BetRequest req = new BetRequest("u", 7, 500L,
            (short) 30, (short) 1, (short) 1, false, false);

        BetAcceptResult res = acceptor.accept(req, round, ledger, wallet, null);

        // Code 1 per TXR:437-438.
        assertThat(res.errorCode).isEqualTo(1);

        // Wallet saw exactly one debit + one refund credit (same txId).
        assertThat(wallet.debitCount()).isEqualTo(1);
        assertThat(wallet.creditCount()).isEqualTo(1);

        InMemoryWalletPort.Call debit = wallet.calls.get(0);
        InMemoryWalletPort.Call refund = wallet.calls.get(1);
        assertThat(debit.kind).isEqualTo("debit");
        assertThat(debit.source).isEqualTo("TaiXiu");
        assertThat(debit.transKind).isEqualTo(TransKind.START);
        assertThat(refund.kind).isEqualTo("credit");
        assertThat(refund.source).isEqualTo("TaiXiuHoanTien");
        assertThat(refund.transKind).isEqualTo(TransKind.END);
        assertThat(refund.txId).as("refund reuses bet txId (TXR:440)")
            .isEqualTo(debit.txId);

        // Pot must NOT contain the bet — race aborted before append.
        assertThat(ledger.potTai().totalValue()).isZero();
    }

    @Test
    void roundLockedBeforeBetReturnsCode2() {
        // Sanity: bet against an already-locked round → code 2, no debit.
        TaiXiuRound round = new TaiXiuRound(new RevealClock.SimpleRevealClock(), new NoOpCachePort());
        round.lockBetting();
        BetLedger ledger = new BetLedger();
        InMemoryWalletPort wallet = new InMemoryWalletPort().seed("u", 10_000L);
        BetAcceptor acceptor = new BetAcceptor();

        BetAcceptResult res = acceptor.accept(
            new BetRequest("u", 1, 500L, (short) 30, (short) 1, (short) 1, false, false),
            round, ledger, wallet, null);

        assertThat(res.errorCode).isEqualTo(2);
        assertThat(wallet.debitCount()).isZero();
    }
}
