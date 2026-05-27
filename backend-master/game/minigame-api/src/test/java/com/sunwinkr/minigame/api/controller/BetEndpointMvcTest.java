package com.sunwinkr.minigame.api.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hazelcast.core.HazelcastInstance;
import com.sunwinkr.minigame.engine.bet.BetAcceptResult;
import com.sunwinkr.minigame.engine.bet.BetAcceptor;
import com.sunwinkr.minigame.engine.bet.BetLedger;
import com.sunwinkr.minigame.engine.bet.BetRequest;
import com.sunwinkr.minigame.engine.bet.TransactionTaiXiuDetail;
import com.sunwinkr.minigame.engine.core.TaiXiuRound;
import com.sunwinkr.minigame.engine.port.BetRecorder;
import com.sunwinkr.minigame.engine.port.WalletPort;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * MVC slice test for POST /api/v2/taixiu/bet.
 * Covers: 200+errorCode=0, errorCode=4 (below min), 403 no token.
 *
 * Plan §5.2 / spec cases B1-B5.
 */
@WebMvcTest(TaiXiuController.class)
@Import(TestSecurityConfig.class)
@ActiveProfiles("test")
class BetEndpointMvcTest {

    @Autowired MockMvc mvc;
    @Autowired ObjectMapper mapper;

    @MockBean HazelcastInstance hazelcast;  // required by AccessTokenFilter component scan
    @MockBean com.sunwinkr.minigame.api.security.RoleResolver roleResolver;  // required by AccessTokenFilter
    @MockBean TaiXiuRound round;
    @MockBean BetAcceptor betAcceptor;
    @MockBean BetLedger ledger;
    @MockBean WalletPort wallet;
    @MockBean BetRecorder recorder;
    @MockBean com.sunwinkr.minigame.api.scheduler.TaiXiuRoundState schedulerState;

    @Test
    @WithMockUser(username = "player1", authorities = {"ROLE_PLAYER"})
    void bet_ok_returns_200_errorCode_0() throws Exception {
        TransactionTaiXiuDetail detail = new TransactionTaiXiuDetail(
            1L, 0, "player1", 1000L, 1, 5, 1, 50000L, 99L);
        BetAcceptResult ok = BetAcceptResult.ok(49000L, 99L, detail);
        when(betAcceptor.accept(
            ArgumentMatchers.any(BetRequest.class),
            ArgumentMatchers.any(),
            ArgumentMatchers.any(),
            ArgumentMatchers.any(),
            ArgumentMatchers.any()))
            .thenReturn(ok);

        String body = "{\"moneyType\":1,\"betValue\":1000,\"betSide\":1}";
        mvc.perform(post("/api/v2/taixiu/bet")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.errorCode").value(0))
            .andExpect(jsonPath("$.currentMoney").value(49000));
    }

    @Test
    @WithMockUser(username = "player1", authorities = {"ROLE_PLAYER"})
    void bet_belowMin_returns_errorCode_4() throws Exception {
        BetAcceptResult err = BetAcceptResult.error(4, 50000L);
        when(betAcceptor.accept(
            ArgumentMatchers.any(),
            ArgumentMatchers.any(),
            ArgumentMatchers.any(),
            ArgumentMatchers.any(),
            ArgumentMatchers.any()))
            .thenReturn(err);

        String body = "{\"moneyType\":1,\"betValue\":10,\"betSide\":0}";
        mvc.perform(post("/api/v2/taixiu/bet")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
            .andExpect(jsonPath("$.errorCode").value(4));
    }

    @Test
    @WithMockUser(username = "player1", authorities = {"ROLE_PLAYER"})
    void bet_windowClosed_returns_errorCode_7() throws Exception {
        // Engine returns code 7 when the timestamp guard fires (SUN-1339 §A2)
        BetAcceptResult err = BetAcceptResult.error(7, 50000L);
        when(betAcceptor.accept(
            ArgumentMatchers.any(),
            ArgumentMatchers.any(),
            ArgumentMatchers.any(),
            ArgumentMatchers.any(),
            ArgumentMatchers.any()))
            .thenReturn(err);

        String body = "{\"moneyType\":1,\"betValue\":1000,\"betSide\":1}";
        mvc.perform(post("/api/v2/taixiu/bet")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
            .andExpect(jsonPath("$.errorCode").value("0007"))
            .andExpect(jsonPath("$.message").value("BET_WINDOW_CLOSED"));
    }

    @Test
    void bet_noToken_returns_403() throws Exception {
        // Spring Security returns 403 (not 401) for anonymous users by default
        // when there is no WWW-Authenticate challenge configured.
        // The controller also explicitly returns 401 via ResponseEntity, but
        // Spring Security's filter chain fires first for unauthenticated requests.
        String body = "{\"moneyType\":1,\"betValue\":1000,\"betSide\":1}";
        mvc.perform(post("/api/v2/taixiu/bet")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
            .andExpect(status().isForbidden());
    }
}
