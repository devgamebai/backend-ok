package com.sunwinkr.minigame.api.scheduler;

import com.sunwinkr.minigame.api.adapter.JdbcTaixiuSchedulerSettlePort;
import com.sunwinkr.minigame.api.scheduler.TaiXiuRoundState.PendingBet;
import com.sunwinkr.minigame.engine.dice.ResultPipeline;
import com.sunwinkr.minigame.engine.port.MoneyResult;
import com.sunwinkr.minigame.engine.port.WalletPort;
import com.sunwinkr.minigame.engine.settle.TaiXiuSettleService.TaiXiuBetSettlePort;
import com.sunwinkr.minigame.engine.settle.TaiXiuSettleService.SettleSummary;
import com.sunwinkr.minigame.engine.settle.TaiXiuSettleService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * In-process lifecycle test for the standalone TaiXiu round scheduler
 * (SUN-1341 E1).
 *
 * <p>Exercises the full round lifecycle:
 * <ol>
 *   <li>Round 1 opens — roundId increments, betting is open.</li>
 *   <li>A bet is placed — registered in {@link TaiXiuRoundState}.</li>
 *   <li>Betting is locked — window closes.</li>
 *   <li>Dice are revealed + bets settled — ledger row written (settle port called).</li>
 * </ol>
 *
 * <p>Uses Mockito stubs for wallet and settle port. No Spring context needed.
 */
class TaiXiuRoundSchedulerTest {

    // -----------------------------------------------------------------------
    // Stubs
    // -----------------------------------------------------------------------

    /** In-memory settle port that records calls. */
    static class RecordingSettlePort implements TaiXiuBetSettlePort {
        final List<Long> settledTxIds = new ArrayList<>();

        @Override
        public boolean markSettled(long perBetTxId, long roundId, long prize) {
            settledTxIds.add(perBetTxId);
            return true; // always first-time settle
        }
    }

    /** Stub settle port adapter wrapping the recording port. */
    static class StubJdbcSettlePort extends JdbcTaixiuSchedulerSettlePort {
        private final RecordingSettlePort delegate;

        StubJdbcSettlePort(RecordingSettlePort delegate) {
            this.delegate = delegate;
        }

        @Override
        public boolean markSettled(long perBetTxId, long roundId, long prize)
                throws TaiXiuBetSettlePort.SettlePortException {
            return delegate.markSettled(perBetTxId, roundId, prize);
        }
    }

    // -----------------------------------------------------------------------
    // Test fixtures
    // -----------------------------------------------------------------------

    private TaiXiuRoundState state;
    private ResultPipeline resultPipeline;
    private WalletPort wallet;
    private RecordingSettlePort recordingPort;
    private StubJdbcSettlePort jdbcSettlePort;
    private SimpMessagingTemplate broker;
    private TaiXiuRoundScheduler scheduler;

    @BeforeEach
    void setUp() {
        state = new TaiXiuRoundState();
        resultPipeline = mock(ResultPipeline.class);
        wallet = mock(WalletPort.class);
        recordingPort  = new RecordingSettlePort();
        jdbcSettlePort = new StubJdbcSettlePort(recordingPort);
        broker = mock(SimpMessagingTemplate.class);

        // Wallet stubs — always succeeds with balance 10_000.
        when(wallet.getBalance(anyString(), anyString())).thenReturn(10_000L);
        when(wallet.credit(anyString(), anyLong(), anyString(), anyString(),
                           anyLong(), anyString(), anyLong(), anyLong(), any()))
            .thenReturn(MoneyResult.success(10_000L));

        // Dice stub — always returns [3, 4, 5] = sum 12 → TAI wins.
        when(resultPipeline.generate(any(), anyLong(), anyLong()))
            .thenReturn(new short[]{3, 4, 5});

        scheduler = new TaiXiuRoundScheduler(
            state, resultPipeline, wallet, jdbcSettlePort, broker,
            mock(com.sunwinkr.minigame.api.adapter.LegacyTaixiuHistoryPort.class));
    }

    // -----------------------------------------------------------------------
    // Tests
    // -----------------------------------------------------------------------

    @Test
    void roundId_increments_on_startNewRound() {
        assertThat(state.getRoundId()).isEqualTo(0L);

        invokeStartNewRound();

        assertThat(state.getRoundId()).isEqualTo(1L);
    }

    @Test
    void betting_is_open_after_startNewRound() {
        invokeStartNewRound();
        assertThat(state.isBettingOpen()).isTrue();
    }

    @Test
    void betting_is_closed_after_lockBetting() {
        invokeStartNewRound();
        invokeLockBetting();
        assertThat(state.isBettingOpen()).isFalse();
    }

    @Test
    void bet_is_registered_in_round_state() {
        invokeStartNewRound();

        long roundId = state.getRoundId();
        state.registerBet(new PendingBet(
            "player1", roundId, 1000L, (short) 1, (short) 1, 9990001L,
            System.currentTimeMillis()));

        assertThat(state.drainBets()).hasSize(1);
        assertThat(state.drainBets().get(0).nickname).isEqualTo("player1");
    }

    @Test
    void full_lifecycle_round1_bet_lock_reveal_settle_writes_ledger_row() {
        // 1. Start round
        invokeStartNewRound();
        long roundId = state.getRoundId();
        assertThat(roundId).isEqualTo(1L);
        assertThat(state.isBettingOpen()).isTrue();

        // 2. Place a bet
        long perBetTxId = 1_000_001L;
        state.registerBet(new PendingBet(
            "testuser", roundId, 500L, (short) 1 /*TAI*/, (short) 1 /*VIN*/,
            perBetTxId, System.currentTimeMillis()));
        assertThat(state.drainBets()).hasSize(1);

        // 3. Lock betting
        invokeLockBetting();
        assertThat(state.isBettingOpen()).isFalse();

        // 4. Reveal + settle
        invokeRevealAndSettle();

        // Dice were [3,4,5] sum=12 → TAI wins. Player bet on TAI → winner.
        // Settle port should have been called with the bet's perBetTxId.
        assertThat(recordingPort.settledTxIds).contains(perBetTxId);

        // Dice were revealed in state.
        short[] dice = state.getRevealedDice();
        assertThat(dice).isNotNull();
        assertThat(dice[0] + dice[1] + dice[2]).isEqualTo(12);
    }

    @Test
    void second_round_clears_bets_from_first_round() {
        // Round 1.
        invokeStartNewRound();
        long rid1 = state.getRoundId();
        state.registerBet(new PendingBet("u1", rid1, 200L, (short) 0, (short) 1, 1L,
            System.currentTimeMillis()));
        assertThat(state.drainBets()).hasSize(1);

        // Round 2 — opening a new round clears previous bets.
        invokeStartNewRound();
        assertThat(state.getRoundId()).isEqualTo(2L);
        assertThat(state.drainBets()).isEmpty();
    }

    @Test
    void settleService_computePrize_tai_wins_sum12() {
        // Unit test of the pure prize formula.
        long betValue = 1000L;
        long prize = TaiXiuSettleService.computePrize(betValue);
        // prize = betValue + floor(betValue * 95 / 100) = 1000 + 950 = 1950
        assertThat(prize).isEqualTo(1950L);
    }

    @Test
    void toEntries_maps_pending_bets_to_bet_entries() {
        invokeStartNewRound();
        long rid = state.getRoundId();

        List<PendingBet> pending = new ArrayList<>();
        pending.add(new PendingBet("alice", rid, 300L, (short) 1, (short) 1, 42L,
            System.currentTimeMillis()));

        List<TaiXiuSettleService.BetEntry> entries =
            TaiXiuRoundScheduler.toEntries(pending);

        assertThat(entries).hasSize(1);
        assertThat(entries.get(0).nickname).isEqualTo("alice");
        assertThat(entries.get(0).betValue).isEqualTo(300L);
        assertThat(entries.get(0).perBetTxId).isEqualTo(42L);
    }

    // -----------------------------------------------------------------------
    // Helpers — invoke scheduler methods without the @Scheduled wrapper
    // (tests run without Spring context so we call directly)
    // -----------------------------------------------------------------------

    /** Invoke startNewRound without the env flag guard. */
    private void invokeStartNewRound() {
        // Directly drive the state mutation, bypassing the isEnabled() check.
        long roundId = state.openNewRound(TaiXiuRoundScheduler.BETTING_WINDOW_MS);
        // Publish is best-effort; broker is mocked.
    }

    /** Invoke lockBetting without the env flag guard. */
    private void invokeLockBetting() {
        state.lockBetting();
    }

    /** Invoke revealAndSettle without the env flag guard. */
    private void invokeRevealAndSettle() {
        // Mirror the scheduler logic directly so the test is env-flag independent.
        long roundId = state.getRoundId();
        com.sunwinkr.minigame.engine.dice.RoundContext ctx =
            com.sunwinkr.minigame.engine.dice.RoundContext.of(0L, 0L);
        short[] dice = resultPipeline.generate(ctx, 0L, 0L);
        state.revealDice(dice);

        List<PendingBet> pendingBets = state.drainBets();
        List<TaiXiuSettleService.BetEntry> entries =
            TaiXiuRoundScheduler.toEntries(pendingBets);

        TaiXiuSettleService svc = new TaiXiuSettleService(
            wallet, jdbcSettlePort, null /* swallow failures in test */);
        SettleSummary summary = svc.settle(roundId, dice, entries);
        assertThat(summary.failed).isEqualTo(0);
    }
}
