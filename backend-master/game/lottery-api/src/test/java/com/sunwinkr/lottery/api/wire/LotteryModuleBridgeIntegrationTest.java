package com.sunwinkr.lottery.api.wire;

import com.sunwinkr.lottery.engine.bet.BetAcceptResult;
import com.sunwinkr.lottery.engine.bet.BetAcceptor;
import com.sunwinkr.lottery.engine.bet.BetRequest;
import com.sunwinkr.lottery.engine.port.SettledFlagStore;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.Clock;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Integration test for {@link LotteryModuleBridge#bet} — asserts the
 * legacy BitZero field set (nickname, userId, mode, num, betValue)
 * threads through to {@link BetAcceptor#accept(BetRequest)} unchanged
 * and the {@link BetAcceptResult} maps back to a {@link
 * LotteryModuleBridge.BridgeBetResponse} with the right errorCode +
 * currentMoney + ticketId.
 *
 * <p>Plan §6.
 */
class LotteryModuleBridgeIntegrationTest {

    @Test
    void bet_endToEnd_passesFieldsToEngine_andMapsResultBack() {
        BetAcceptor acceptor = mock(BetAcceptor.class);
        SettledFlagStore settled = mock(SettledFlagStore.class);
        when(acceptor.accept(any(BetRequest.class)))
            .thenReturn(BetAcceptResult.ok(49000L, 1234L));

        LotteryModuleBridge bridge = new LotteryModuleBridge(acceptor, settled, Clock.systemUTC());
        LotteryModuleBridge.BridgeBetResponse out = bridge.bet("player1", 7L, 1, "27", 1000L);

        // Engine received the legacy fields verbatim.
        ArgumentCaptor<BetRequest> cap = ArgumentCaptor.forClass(BetRequest.class);
        verify(acceptor).accept(cap.capture());
        BetRequest req = cap.getValue();
        assertThat(req.getNickname()).isEqualTo("player1");
        assertThat(req.getUserId()).isEqualTo(7L);
        assertThat(req.getModeId()).isEqualTo(1);
        assertThat(req.getTicket()).isEqualTo("27");
        assertThat(req.getBetValue()).isEqualTo(1000L);

        // Engine result mapped back into the bridge response.
        assertThat(out.errorCode).isEqualTo("0000");
        assertThat(out.currentMoney).isEqualTo(49000L);
        assertThat(out.ticketId).isEqualTo(1234L);
    }

    @Test
    void bet_engineRejects_mapsErrorCode() {
        BetAcceptor acceptor = mock(BetAcceptor.class);
        SettledFlagStore settled = mock(SettledFlagStore.class);
        when(acceptor.accept(any(BetRequest.class))).thenReturn(BetAcceptResult.locked());

        LotteryModuleBridge bridge = new LotteryModuleBridge(acceptor, settled, Clock.systemUTC());
        LotteryModuleBridge.BridgeBetResponse out = bridge.bet("player1", 7L, 1, "27", 1000L);

        assertThat(out.errorCode).isEqualTo("0002"); // locked
    }

    @Test
    void bet_nullNickname_safelyRejected() {
        BetAcceptor acceptor = mock(BetAcceptor.class);
        SettledFlagStore settled = mock(SettledFlagStore.class);
        LotteryModuleBridge bridge = new LotteryModuleBridge(acceptor, settled, Clock.systemUTC());
        LotteryModuleBridge.BridgeBetResponse out = bridge.bet(null, 0L, 1, "27", 1000L);
        assertThat(out.errorCode).isEqualTo("0001");
    }
}
