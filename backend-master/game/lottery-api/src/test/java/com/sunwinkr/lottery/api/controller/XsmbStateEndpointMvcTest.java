package com.sunwinkr.lottery.api.controller;

import com.hazelcast.core.HazelcastInstance;
import com.sunwinkr.lottery.engine.bet.BetAcceptor;
import com.sunwinkr.lottery.engine.clock.LotteryClock;
import com.sunwinkr.lottery.engine.port.BetStore;
import com.sunwinkr.lottery.engine.port.ResultStore;
import com.sunwinkr.lottery.engine.port.SettledFlagStore;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * MVC slice for GET /api/v2/lottery/xsmb/state. Verifies phase derivation
 * (DRAW_PENDING when bets open + not settled; SETTLED when settle complete)
 * and VN clock surfacing.
 */
@WebMvcTest(XsmbController.class)
@Import(TestSecurityConfig.class)
class XsmbStateEndpointMvcTest {

    @Autowired MockMvc mvc;

    @MockBean HazelcastInstance hazelcast;
    @MockBean com.sunwinkr.lottery.api.security.RoleResolver roleResolver;
    @MockBean BetAcceptor betAcceptor;
    @MockBean BetStore bets;
    @MockBean ResultStore results;
    @MockBean SettledFlagStore settledFlag;
    @MockBean Clock clock;

    private void wireClockAt(LocalDateTime vnTime) {
        // Pin the clock so withZone(VN) returns a real Clock at the given VN
        // wall time. The controller calls clock.withZone(VN) and then
        // LocalDate.now(returnedClock) — so the returned clock MUST carry
        // the Hanoi zone, not UTC.
        Clock fixedAtVn = Clock.fixed(vnTime.atZone(LotteryClock.VN).toInstant(), LotteryClock.VN);
        when(clock.withZone(any(ZoneId.class))).thenReturn(fixedAtVn);
        when(clock.instant()).thenReturn(fixedAtVn.instant());
        when(clock.getZone()).thenReturn(fixedAtVn.getZone());
    }

    @Test
    @WithMockUser(username = "player1", authorities = {"ROLE_PLAYER"})
    void state_morningPreLock_returns_DRAW_PENDING_open() throws Exception {
        // Clock: 2026-05-14 10:00 VN — next lock=18:10 VN = 1778757000000 ms UTC
        //                              next scrape=18:35 VN = 1778758500000 ms UTC
        wireClockAt(LocalDateTime.of(2026, 5, 14, 10, 0));
        when(settledFlag.isSettled(any(LocalDate.class))).thenReturn(false);
        mvc.perform(get("/api/v2/lottery/xsmb/state"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.phase").value("DRAW_PENDING"))
            .andExpect(jsonPath("$.bettingOpen").value(true))
            // lockTime + alias safeBetExpiresAt — epoch-ms for 18:10 Hanoi same day
            .andExpect(jsonPath("$.lockTime").value(1778757000000L))
            .andExpect(jsonPath("$.safeBetExpiresAt").value(1778757000000L))
            // scrapeTime + alias settleAt — epoch-ms for 18:35 Hanoi same day
            .andExpect(jsonPath("$.scrapeTime").value(1778758500000L))
            .andExpect(jsonPath("$.settleAt").value(1778758500000L))
            // roundId — Vietnamese date yyyymmdd numeric
            .andExpect(jsonPath("$.roundId").value(20260514));
    }

    @Test
    @WithMockUser(username = "player1", authorities = {"ROLE_PLAYER"})
    void state_postLockPreSettle_returns_DRAW_LOCKED_closed() throws Exception {
        wireClockAt(LocalDateTime.of(2026, 5, 14, 18, 20));
        when(settledFlag.isSettled(any(LocalDate.class))).thenReturn(false);
        mvc.perform(get("/api/v2/lottery/xsmb/state"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.phase").value("DRAW_LOCKED"))
            .andExpect(jsonPath("$.bettingOpen").value(false));
    }
}
