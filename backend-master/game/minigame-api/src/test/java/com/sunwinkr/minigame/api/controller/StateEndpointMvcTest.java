package com.sunwinkr.minigame.api.controller;

import com.hazelcast.core.HazelcastInstance;
import com.sunwinkr.minigame.engine.bet.BetAcceptor;
import com.sunwinkr.minigame.engine.bet.BetLedger;
import com.sunwinkr.minigame.engine.core.TaiXiuRound;
import com.sunwinkr.minigame.engine.port.BetRecorder;
import com.sunwinkr.minigame.engine.port.WalletPort;
import com.sunwinkr.minigame.engine.snapshot.TaiXiuSnapshot;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * MVC slice test for GET /api/v2/taixiu/state.
 * Verifies dice fields are censored (0) pre-reveal per spec INV-14.
 * Plan §5.1.
 */
@WebMvcTest(TaiXiuController.class)
@Import(TestSecurityConfig.class)
@ActiveProfiles("test")
class StateEndpointMvcTest {

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
    void state_preReveal_diceAreZero() throws Exception {
        TaiXiuSnapshot snap = new TaiXiuSnapshot();
        snap.referenceId = 42L;
        snap.roundId = 42L;
        snap.bettingClosesAt = 1_700_000_000_000L;
        snap.bettingState = true;
        snap.remainTime = (short) 25;
        // dice1/2/3 default to 0 (pre-reveal, censored by engine)
        snap.dice1 = 0;
        snap.dice2 = 0;
        snap.dice3 = 0;

        when(round.snapshotForClient(anyString())).thenReturn(snap);

        mvc.perform(get("/api/v2/taixiu/state?moneyType=1"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.referenceId").value(42))
            .andExpect(jsonPath("$.roundId").value(42))
            .andExpect(jsonPath("$.safeBetExpiresAt").value(1_700_000_000_000L))
            .andExpect(jsonPath("$.bettingState").value(true))
            .andExpect(jsonPath("$.dice1").value(0))
            .andExpect(jsonPath("$.dice2").value(0))
            .andExpect(jsonPath("$.dice3").value(0));
    }

    @Test
    void state_noToken_returns_403() throws Exception {
        // Spring Security returns 403 for unauthenticated access to authenticated endpoint.
        mvc.perform(get("/api/v2/taixiu/state"))
            .andExpect(status().isForbidden());
    }
}
