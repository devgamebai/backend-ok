package com.sunwinkr.minigame.api.controller.sicbo;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hazelcast.core.HazelcastInstance;
import com.sunwinkr.minigame.api.adapter.sicbo.LegacySicboHistoryPort;
import com.sunwinkr.minigame.engine.port.BetRecorder;
import com.sunwinkr.minigame.engine.port.WalletPort;
import com.sunwinkr.minigame.engine.sicbo.bet.SicboBetAcceptResult;
import com.sunwinkr.minigame.engine.sicbo.bet.SicboBetRequest;
import com.sunwinkr.minigame.engine.sicbo.bet.SicboBetService;
import com.sunwinkr.minigame.engine.sicbo.bet.SicboBetType;
import com.sunwinkr.minigame.engine.sicbo.bet.SicboPotState;
import com.sunwinkr.minigame.engine.sicbo.bet.SicboTxIdGenerator;
import com.sunwinkr.minigame.engine.sicbo.core.SicboRound;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatchers;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * MVC slice tests for POST /api/v2/sicbo/bet.
 *
 * <p>Covers: 200+success, 400 invalid betSide name (errorCode 6),
 * 403 no-token, and the string→numeric ID mapping ("TAI"→48,
 * "ONE_DICE_3"→17). Plan §6 / spec cases B1-B6.
 */
@WebMvcTest(SicboController.class)
@Import(TestSicboSecurityConfig.class)
@ActiveProfiles("test")
class SicboBetEndpointMvcTest {

    @Autowired MockMvc mvc;
    @Autowired ObjectMapper mapper;

    @MockBean HazelcastInstance hazelcast;
    @MockBean com.sunwinkr.minigame.api.security.RoleResolver roleResolver;
    @MockBean SicboRound round;
    @MockBean SicboBetService betService;
    @MockBean SicboPotState pot;
    @MockBean SicboTxIdGenerator txGen;
    @MockBean(name = "sicboWalletPort") WalletPort wallet;
    @MockBean(name = "sicboBetRecorder") BetRecorder recorder;
    @MockBean LegacySicboHistoryPort legacyHistory;

    @Test
    @WithMockUser(username = "player1", authorities = {"ROLE_PLAYER"})
    void bet_ok_returns_200_errorCode_0000() throws Exception {
        SicboBetAcceptResult ok = SicboBetAcceptResult.success(49000L, 99L, "42-1", 48);
        when(betService.accept(
            ArgumentMatchers.any(SicboBetRequest.class),
            ArgumentMatchers.any(), ArgumentMatchers.any(),
            ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any()))
            .thenReturn(ok);

        String body = "{\"moneyType\":1,\"betValue\":1000,\"betSide\":\"TAI\"}";
        mvc.perform(post("/api/v2/sicbo/bet")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.errorCode").value("0000"))
            .andExpect(jsonPath("$.currentMoney").value(49000))
            .andExpect(jsonPath("$.betSideId").value(48));
    }

    @Test
    @WithMockUser(username = "player1", authorities = {"ROLE_PLAYER"})
    void bet_invalidBetSideName_errorCode_0006() throws Exception {
        SicboBetAcceptResult err = SicboBetAcceptResult.error(6);
        when(betService.accept(
            ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any(),
            ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any()))
            .thenReturn(err);

        String body = "{\"moneyType\":1,\"betValue\":1000,\"betSide\":\"NOT_A_REAL_SIDE\"}";
        mvc.perform(post("/api/v2/sicbo/bet")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
            .andExpect(jsonPath("$.errorCode").value("0006"));
    }

    @Test
    @WithMockUser(username = "player1", authorities = {"ROLE_PLAYER"})
    void bet_windowClosed_returns_errorCode_7() throws Exception {
        // Engine returns code 7 when the timestamp guard fires (SUN-1339 §A3)
        SicboBetAcceptResult err = SicboBetAcceptResult.error(7, 50000L);
        when(betService.accept(
            ArgumentMatchers.any(SicboBetRequest.class),
            ArgumentMatchers.any(), ArgumentMatchers.any(),
            ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any()))
            .thenReturn(err);

        String body = "{\"moneyType\":1,\"betValue\":1000,\"betSide\":\"TAI\"}";
        mvc.perform(post("/api/v2/sicbo/bet")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
            .andExpect(jsonPath("$.errorCode").value("0007"))
            .andExpect(jsonPath("$.message").value("BET_WINDOW_CLOSED"));
    }

    @Test
    void bet_noToken_returns_403() throws Exception {
        String body = "{\"moneyType\":1,\"betValue\":1000,\"betSide\":\"TAI\"}";
        mvc.perform(post("/api/v2/sicbo/bet")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
            .andExpect(status().isForbidden());
    }

    /**
     * String-bet-side mapping: confirms the controller forwards the raw
     * name into the engine, and the engine's static SicboBetType lookup
     * maps "TAI" → id 48 and "ONE_DICE_3" → id 17. We assert the static
     * lookup directly (since the controller mocks betService) so the test
     * pins the wire contract even if betService is stubbed.
     */
    @Test
    @WithMockUser(username = "player1", authorities = {"ROLE_PLAYER"})
    void stringBetSideMapping_TAI_to_48_and_ONE_DICE_3_to_17() throws Exception {
        // SicboBetType static map is the contract.
        assertThat(SicboBetType.byName("TAI").getId()).isEqualTo(48);
        assertThat(SicboBetType.byName("ONE_DICE_3").getId()).isEqualTo(17);

        SicboBetAcceptResult ok = SicboBetAcceptResult.success(0L, 1L, "1-1", 17);
        when(betService.accept(
            ArgumentMatchers.any(SicboBetRequest.class),
            ArgumentMatchers.any(), ArgumentMatchers.any(),
            ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any()))
            .thenReturn(ok);

        // Issue a request with "ONE_DICE_3" and capture the engine req
        // shape to confirm the controller forwarded the string unchanged.
        String body = "{\"moneyType\":1,\"betValue\":1000,\"betSide\":\"ONE_DICE_3\"}";
        mvc.perform(post("/api/v2/sicbo/bet")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.betSideId").value(17));

        ArgumentCaptor<SicboBetRequest> captor = ArgumentCaptor.forClass(SicboBetRequest.class);
        verify(betService).accept(captor.capture(),
            ArgumentMatchers.any(), ArgumentMatchers.any(),
            ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any());
        assertThat(captor.getValue().betSideName).isEqualTo("ONE_DICE_3");
    }
}
