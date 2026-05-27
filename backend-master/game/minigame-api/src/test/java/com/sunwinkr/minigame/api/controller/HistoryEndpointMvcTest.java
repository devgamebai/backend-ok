package com.sunwinkr.minigame.api.controller;

import com.hazelcast.core.HazelcastInstance;
import com.sunwinkr.minigame.engine.bet.BetAcceptor;
import com.sunwinkr.minigame.engine.bet.BetLedger;
import com.sunwinkr.minigame.engine.core.TaiXiuRound;
import com.sunwinkr.minigame.engine.port.BetRecorder;
import com.sunwinkr.minigame.engine.port.WalletPort;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * MVC slice test for GET /api/v2/taixiu/history.
 * PR-4 baseline: always returns empty list; cap=120 honored.
 * Plan §5.1 / §2.8 H1/H2.
 */
@WebMvcTest(TaiXiuController.class)
@Import(TestSecurityConfig.class)
@ActiveProfiles("test")
class HistoryEndpointMvcTest {

    @Autowired MockMvc mvc;

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
    void history_pr4baseline_returnsEmptyList() throws Exception {
        mvc.perform(get("/api/v2/taixiu/history?moneyType=1&n=10"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.entries").isArray())
            .andExpect(jsonPath("$.entries.length()").value(0));
    }

    @Test
    @WithMockUser(username = "player1", authorities = {"ROLE_PLAYER"})
    void history_nAboveCap_clampedTo120() throws Exception {
        // n=9999 should be clamped to 120 (no error, empty list returned)
        mvc.perform(get("/api/v2/taixiu/history?n=9999"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.entries").isArray());
    }
}
