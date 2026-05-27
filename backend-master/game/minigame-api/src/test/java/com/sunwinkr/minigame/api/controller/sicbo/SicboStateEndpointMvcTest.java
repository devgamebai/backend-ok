package com.sunwinkr.minigame.api.controller.sicbo;

import com.hazelcast.core.HazelcastInstance;
import com.sunwinkr.minigame.api.adapter.sicbo.LegacySicboHistoryPort;
import com.sunwinkr.minigame.engine.core.RevealPhase;
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

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * GET /api/v2/sicbo/state — verifies dice fields are censored (null)
 * pre-reveal (snapshot phase OPEN). Plan §6 / INV-14.
 */
@WebMvcTest(SicboController.class)
@Import(TestSicboSecurityConfig.class)
@ActiveProfiles("test")
class SicboStateEndpointMvcTest {

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
    void state_preReveal_diceAreNull() throws Exception {
        when(round.getReferenceId()).thenReturn(42L);
        when(round.bettingClosesAt()).thenReturn(1_700_000_000_000L);
        when(round.isBetting()).thenReturn(true);
        when(round.getPhase()).thenReturn(RevealPhase.OPEN);
        when(round.getPendingDice()).thenReturn(null);

        mvc.perform(get("/api/v2/sicbo/state?moneyType=1"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.referenceId").value(42))
            .andExpect(jsonPath("$.roundId").value(42))
            .andExpect(jsonPath("$.safeBetExpiresAt").value(1_700_000_000_000L))
            .andExpect(jsonPath("$.bettingState").value(true))
            .andExpect(jsonPath("$.dice1").doesNotExist())
            .andExpect(jsonPath("$.dice2").doesNotExist())
            .andExpect(jsonPath("$.dice3").doesNotExist())
            .andExpect(jsonPath("$.phase").value("OPEN"));
    }
}
