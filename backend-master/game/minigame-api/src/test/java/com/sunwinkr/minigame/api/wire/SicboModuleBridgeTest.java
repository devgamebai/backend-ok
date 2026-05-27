package com.sunwinkr.minigame.api.wire;

import com.sunwinkr.minigame.api.push.SicboTickPublisher;
import com.sunwinkr.minigame.engine.core.RevealPhase;
import com.sunwinkr.minigame.engine.port.BetRecorder;
import com.sunwinkr.minigame.engine.port.WalletPort;
import com.sunwinkr.minigame.engine.sicbo.bet.SicboBetAcceptResult;
import com.sunwinkr.minigame.engine.sicbo.bet.SicboBetRequest;
import com.sunwinkr.minigame.engine.sicbo.bet.SicboBetService;
import com.sunwinkr.minigame.engine.sicbo.bet.SicboPotState;
import com.sunwinkr.minigame.engine.sicbo.bet.SicboTxIdGenerator;
import com.sunwinkr.minigame.engine.sicbo.core.SicboRound;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Engine event listener tests for {@link SicboModuleBridge}.
 *
 * <p>Verifies that engine-side state changes (bet accepted, dice
 * revealed, new round) are translated into the legacy BitZero-style
 * bridge responses + STOMP pushes.
 */
class SicboModuleBridgeTest {

    @Test
    void betAcceptedProducesLegacyBetResponse() {
        SicboRound round = mock(SicboRound.class);
        SicboBetService svc = mock(SicboBetService.class);
        SicboPotState pot = mock(SicboPotState.class);
        SicboTxIdGenerator txGen = mock(SicboTxIdGenerator.class);
        WalletPort wallet = mock(WalletPort.class);
        BetRecorder rec = mock(BetRecorder.class);
        SicboTickPublisher push = mock(SicboTickPublisher.class);

        SicboBetAcceptResult ok = SicboBetAcceptResult.success(40000L, 7L, "42-2", 48);
        when(svc.accept(any(SicboBetRequest.class), any(), any(), any(), any(), any())).thenReturn(ok);
        when(pot.totalValueBetUser()).thenReturn(1000L);

        SicboModuleBridge bridge = new SicboModuleBridge(round, svc, pot, txGen, wallet, rec, push);

        SicboModuleBridge.BridgeBetResponse resp = bridge.bet(
            "player1", 0, 1000L, (short) 30, (short) 1, "TAI", false);

        assertThat(resp.error).isEqualTo((byte) 0);
        assertThat(resp.currentMoney).isEqualTo(40000L);
        assertThat(resp.perBetTxId).isEqualTo(7L);
        assertThat(resp.transactionCode).isEqualTo("42-2");
        assertThat(resp.betSideId).isEqualTo(48);

        verify(push).publishPotDelta((short) 1, 1000L);
    }

    @Test
    void onDiceRevealedFiresRevealPush() {
        SicboRound round = mock(SicboRound.class);
        when(round.getReferenceId()).thenReturn(100L);
        when(round.getPhase()).thenReturn(RevealPhase.REVEALED);
        SicboTickPublisher push = mock(SicboTickPublisher.class);

        SicboModuleBridge bridge = new SicboModuleBridge(
            round, mock(SicboBetService.class), mock(SicboPotState.class),
            mock(SicboTxIdGenerator.class), mock(WalletPort.class),
            mock(BetRecorder.class), push);

        bridge.onDiceRevealed(new short[] { 3, 3, 3 });
        verify(push).publishReveal(any());
    }

    @Test
    void onNewRoundFiresRoundStartPush() {
        SicboTickPublisher push = mock(SicboTickPublisher.class);
        SicboModuleBridge bridge = new SicboModuleBridge(
            mock(SicboRound.class), mock(SicboBetService.class), mock(SicboPotState.class),
            mock(SicboTxIdGenerator.class), mock(WalletPort.class),
            mock(BetRecorder.class), push);

        bridge.onNewRound(101L);
        verify(push).publishRoundStart(101L);
    }
}
