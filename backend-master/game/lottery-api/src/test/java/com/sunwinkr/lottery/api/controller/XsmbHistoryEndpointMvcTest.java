package com.sunwinkr.lottery.api.controller;

import com.hazelcast.core.HazelcastInstance;
import com.sunwinkr.lottery.engine.bet.BetAcceptor;
import com.sunwinkr.lottery.engine.model.LotteryTicket;
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
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * MVC slice for GET /api/v2/lottery/xsmb/history. Verifies caller-only
 * access (403 if caller asks for another user's history without admin).
 */
@WebMvcTest(XsmbController.class)
@Import(TestSecurityConfig.class)
class XsmbHistoryEndpointMvcTest {

    @Autowired MockMvc mvc;

    @MockBean HazelcastInstance hazelcast;
    @MockBean com.sunwinkr.lottery.api.security.RoleResolver roleResolver;
    @MockBean BetAcceptor betAcceptor;
    @MockBean BetStore bets;
    @MockBean ResultStore results;
    @MockBean SettledFlagStore settledFlag;
    @MockBean Clock clock;

    @Test
    @WithMockUser(username = "player1", authorities = {"ROLE_PLAYER"})
    void history_self_returns_200_with_ticket_list() throws Exception {
        LotteryTicket t = new LotteryTicket(
            42L, 1L, "player1", 22000L, 1, "27", null,
            LocalDateTime.now(), null, 1000L, 22, 80);
        when(bets.findByUser(eq("player1"), any()))
            .thenReturn(Arrays.asList(t));
        mvc.perform(get("/api/v2/lottery/xsmb/history"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.tickets[0].ticketId").value(42));
    }

    @Test
    @WithMockUser(username = "player1", authorities = {"ROLE_PLAYER"})
    void history_otherUser_nonAdmin_returns_403() throws Exception {
        when(bets.findByUser(eq("alice"), any(BetStore.Paging.class)))
            .thenReturn(Collections.<LotteryTicket>emptyList());
        mvc.perform(get("/api/v2/lottery/xsmb/history").param("userId", "alice"))
            .andExpect(status().isForbidden());
    }
}
