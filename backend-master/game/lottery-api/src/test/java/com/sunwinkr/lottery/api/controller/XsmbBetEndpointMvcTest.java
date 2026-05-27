package com.sunwinkr.lottery.api.controller;

import com.hazelcast.core.HazelcastInstance;
import com.sunwinkr.lottery.engine.bet.BetAcceptResult;
import com.sunwinkr.lottery.engine.bet.BetAcceptor;
import com.sunwinkr.lottery.engine.bet.BetRequest;
import com.sunwinkr.lottery.engine.clock.LotteryClock;
import com.sunwinkr.lottery.engine.port.BetStore;
import com.sunwinkr.lottery.engine.port.ResultStore;
import com.sunwinkr.lottery.engine.port.SettledFlagStore;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneId;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * MVC slice test for POST /api/v2/lottery/xsmb/bet. Plan §5.2 / cases B1-B5.
 * Covers: 200+errorCode=0000, errorCode=0004 invalid mode, 403 no token,
 * and SUN-1339 A4: errorCode=0002 when past safeBetExpiresAt (18:10 VN).
 */
@WebMvcTest(XsmbController.class)
@Import(TestSecurityConfig.class)
class XsmbBetEndpointMvcTest {

    @Autowired MockMvc mvc;

    @MockBean HazelcastInstance hazelcast;
    @MockBean com.sunwinkr.lottery.api.security.RoleResolver roleResolver;
    @MockBean BetAcceptor betAcceptor;
    @MockBean BetStore bets;
    @MockBean ResultStore results;
    @MockBean SettledFlagStore settledFlag;
    @MockBean Clock clock;

    /**
     * Default clock stub: 10:00 VN — well before the 18:10 lock.
     * The bet() handler calls clock.withZone(VN) for the timestamp guard and
     * clock.millis() for the current-time check. Both must be consistent.
     */
    @BeforeEach
    void wireClockMorning() {
        Clock fixedMorning = Clock.fixed(
            LocalDateTime.of(2026, 5, 15, 10, 0).atZone(LotteryClock.VN).toInstant(),
            LotteryClock.VN);
        when(clock.withZone(any(ZoneId.class))).thenReturn(fixedMorning);
        when(clock.millis()).thenReturn(fixedMorning.millis());
        when(clock.instant()).thenReturn(fixedMorning.instant());
        when(clock.getZone()).thenReturn(fixedMorning.getZone());
    }

    @Test
    @WithMockUser(username = "player1", authorities = {"ROLE_PLAYER"})
    void bet_ok_returns_200_errorCode_0000() throws Exception {
        when(betAcceptor.accept(any(BetRequest.class)))
            .thenReturn(BetAcceptResult.ok(49000L, 99L));
        String body = "{\"modeId\":1,\"ticket\":\"27\",\"betValue\":1000}";
        mvc.perform(post("/api/v2/lottery/xsmb/bet")
                .contentType(MediaType.APPLICATION_JSON).content(body))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.errorCode").value("0000"))
            .andExpect(jsonPath("$.currentMoney").value(49000));
    }

    @Test
    @WithMockUser(username = "player1", authorities = {"ROLE_PLAYER"})
    void bet_invalidMode_returns_errorCode_0004() throws Exception {
        when(betAcceptor.accept(any(BetRequest.class)))
            .thenReturn(BetAcceptResult.unknownMode());
        String body = "{\"modeId\":99,\"ticket\":\"27\",\"betValue\":1000}";
        mvc.perform(post("/api/v2/lottery/xsmb/bet")
                .contentType(MediaType.APPLICATION_JSON).content(body))
            .andExpect(jsonPath("$.errorCode").value("0004"));
    }

    @Test
    @WithMockUser(username = "player1", authorities = {"ROLE_PLAYER"})
    void bet_postLock_returns_errorCode_0002_BET_WINDOW_CLOSED() throws Exception {
        // SUN-1339 A4: timestamp guard rejects bets when now >= safeBetExpiresAt.
        Clock fixedPostLock = Clock.fixed(
            LocalDateTime.of(2026, 5, 15, 18, 11).atZone(LotteryClock.VN).toInstant(),
            LotteryClock.VN);
        when(clock.withZone(any(ZoneId.class))).thenReturn(fixedPostLock);
        when(clock.millis()).thenReturn(fixedPostLock.millis());
        String body = "{\"modeId\":1,\"ticket\":\"27\",\"betValue\":1000}";
        mvc.perform(post("/api/v2/lottery/xsmb/bet")
                .contentType(MediaType.APPLICATION_JSON).content(body))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.errorCode").value("0002"));
    }

    @Test
    void bet_noToken_returns_403() throws Exception {
        // Spring Security returns 403 for anonymous users by default.
        String body = "{\"modeId\":1,\"ticket\":\"27\",\"betValue\":1000}";
        mvc.perform(post("/api/v2/lottery/xsmb/bet")
                .contentType(MediaType.APPLICATION_JSON).content(body))
            .andExpect(status().isForbidden());
    }
}
