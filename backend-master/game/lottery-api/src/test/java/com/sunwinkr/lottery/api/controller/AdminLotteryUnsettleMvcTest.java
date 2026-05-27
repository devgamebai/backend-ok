package com.sunwinkr.lottery.api.controller;

import com.hazelcast.core.HazelcastInstance;
import com.sunwinkr.lottery.api.push.SettleAnnouncePublisher;
import com.sunwinkr.lottery.engine.ingest.DrawIngest;
import com.sunwinkr.lottery.engine.model.LotteryTicket;
import com.sunwinkr.lottery.engine.model.SettleStatus;
import com.sunwinkr.lottery.engine.port.BetStore;
import com.sunwinkr.lottery.engine.port.MoneyResult;
import com.sunwinkr.lottery.engine.port.ResultStore;
import com.sunwinkr.lottery.engine.port.SettledFlagStore;
import com.sunwinkr.lottery.engine.port.WalletPort;
import com.sunwinkr.lottery.engine.settle.LotterySettleService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * MVC slice for POST /api/v2/lottery/admin/xsmb/unsettle.
 *
 * Covers:
 *  - 403 for non-admin
 *  - 404 when ticket not found
 *  - 400 NOT_SETTLED when ticket is PENDING
 *  - 200 winner reversal: debit prize, void ticket
 *  - 200 loser reversal: credit bet amount, void ticket
 */
@WebMvcTest(AdminLotteryController.class)
@Import(TestSecurityConfig.class)
class AdminLotteryUnsettleMvcTest {

    @Autowired MockMvc mvc;

    @MockBean HazelcastInstance hazelcast;
    @MockBean com.sunwinkr.lottery.api.security.RoleResolver roleResolver;
    @MockBean LotterySettleService settleService;
    @MockBean DrawIngest drawIngest;
    @MockBean ResultStore results;
    @MockBean SettledFlagStore settledFlag;
    @MockBean BetStore bets;
    @MockBean SettleAnnouncePublisher announcer;
    @MockBean WalletPort walletPort;

    @Test
    @WithMockUser(username = "player1", authorities = {"ROLE_PLAYER"})
    void unsettle_nonAdmin_returns_403() throws Exception {
        mvc.perform(post("/api/v2/lottery/admin/xsmb/unsettle")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"ticketId\":1,\"reason\":\"test\"}"))
            .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "admin1", authorities = {"ROLE_PLAYER", "ROLE_LOTTERY_ADMIN"})
    void unsettle_ticketNotFound_returns_404() throws Exception {
        when(bets.findById(99L)).thenReturn(Optional.empty());

        mvc.perform(post("/api/v2/lottery/admin/xsmb/unsettle")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"ticketId\":99,\"reason\":\"admin error\"}"))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.success").value(false))
            .andExpect(jsonPath("$.errorCode").value("NOT_FOUND"));
    }

    @Test
    @WithMockUser(username = "admin1", authorities = {"ROLE_PLAYER", "ROLE_LOTTERY_ADMIN"})
    void unsettle_ticketNotSettled_returns_400() throws Exception {
        LotteryTicket pending = settledTicket(10L, "alice", 100_000L, 0L, SettleStatus.PENDING);
        when(bets.findById(10L)).thenReturn(Optional.of(pending));

        mvc.perform(post("/api/v2/lottery/admin/xsmb/unsettle")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"ticketId\":10,\"reason\":\"test\"}"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.success").value(false))
            .andExpect(jsonPath("$.errorCode").value("NOT_SETTLED"));

        verify(walletPort, never()).debit(any(), anyLong(), any(), any(), any(), any(), anyLong(), anyLong(), any());
        verify(walletPort, never()).credit(any(), anyLong(), any(), any(), any(), any(), anyLong(), anyLong(), any());
        verify(bets, never()).voidTicket(anyLong());
    }

    @Test
    @WithMockUser(username = "admin1", authorities = {"ROLE_PLAYER", "ROLE_LOTTERY_ADMIN"})
    void admin_unsettle_voids_settled_winner_and_refunds() throws Exception {
        // Winner: prize > 0 → wallet should be DEBITED (prize taken back).
        LotteryTicket winner = settledTicket(20L, "bob", 100_000L, 800_000L, SettleStatus.SETTLED);
        when(bets.findById(20L)).thenReturn(Optional.of(winner));
        when(walletPort.debit(eq("bob"), eq(800_000L), eq("vin"), eq("LoDe Refund"), eq("Lô Đề"),
                any(String.class), eq(0L), anyLong(), any()))
            .thenReturn(MoneyResult.ok(500_000L));
        when(bets.voidTicket(20L)).thenReturn(1);

        mvc.perform(post("/api/v2/lottery/admin/xsmb/unsettle")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"ticketId\":20,\"reason\":\"wrong result\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.ticketId").value(20))
            .andExpect(jsonPath("$.voidedPrize").value(800000))
            .andExpect(jsonPath("$.refundedBet").value(0));

        verify(bets).voidTicket(20L);
    }

    @Test
    @WithMockUser(username = "admin1", authorities = {"ROLE_PLAYER", "ROLE_LOTTERY_ADMIN"})
    void admin_unsettle_voids_settled_loser_and_refunds_bet() throws Exception {
        // Loser: prize = 0 → wallet should be CREDITED (bet amount refunded).
        LotteryTicket loser = settledTicket(30L, "carol", 220_000L, 0L, SettleStatus.SETTLED);
        when(bets.findById(30L)).thenReturn(Optional.of(loser));
        when(walletPort.credit(eq("carol"), eq(220_000L), eq("vin"), eq("LoDe Refund"), eq("Lô Đề"),
                any(String.class), eq(0L), anyLong(), any()))
            .thenReturn(MoneyResult.ok(1_000_000L));
        when(bets.voidTicket(30L)).thenReturn(1);

        mvc.perform(post("/api/v2/lottery/admin/xsmb/unsettle")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"ticketId\":30,\"reason\":\"settle error\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.ticketId").value(30))
            .andExpect(jsonPath("$.voidedPrize").value(0))
            .andExpect(jsonPath("$.refundedBet").value(220000));

        verify(bets).voidTicket(30L);
    }

    // ---------- Helpers ----------

    private static LotteryTicket settledTicket(long id, String nick, long betValue, long prize, SettleStatus status) {
        return new LotteryTicket(
                id, 100L + id, nick, betValue, 1, "42",
                prize, LocalDateTime.now(), LocalDateTime.now(),
                10_000L, 22, 80,
                status);
    }
}
