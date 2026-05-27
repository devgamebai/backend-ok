package com.sunwinkr.minigame.api.controller.sicbo;

import com.hazelcast.core.HazelcastInstance;
import com.sunwinkr.minigame.api.adapter.sicbo.LegacySicboHistoryPort;
import com.sunwinkr.minigame.engine.port.BetRecorder;
import com.sunwinkr.minigame.engine.port.WalletPort;
import com.sunwinkr.minigame.engine.sicbo.bet.SicboBetService;
import com.sunwinkr.minigame.engine.sicbo.bet.SicboPotState;
import com.sunwinkr.minigame.engine.sicbo.bet.SicboTxIdGenerator;
import com.sunwinkr.minigame.engine.sicbo.core.SicboRound;
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
 * GET /api/v2/sicbo/history — PR-4 baseline: empty list, n capped at 120.
 * Plan §6 / §2.8 H1/H2.
 */
@WebMvcTest(SicboController.class)
@Import(TestSicboSecurityConfig.class)
@ActiveProfiles("test")
class SicboHistoryEndpointMvcTest {

    @Autowired MockMvc mvc;

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
    void history_pr4baseline_returnsEmptyList() throws Exception {
        mvc.perform(get("/api/v2/sicbo/history?moneyType=1&n=10"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.entries").isArray())
            .andExpect(jsonPath("$.entries.length()").value(0));
    }
}
