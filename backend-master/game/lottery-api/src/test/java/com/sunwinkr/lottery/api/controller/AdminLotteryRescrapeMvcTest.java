package com.sunwinkr.lottery.api.controller;

import com.hazelcast.core.HazelcastInstance;
import com.sunwinkr.lottery.api.push.SettleAnnouncePublisher;
import com.sunwinkr.lottery.engine.ingest.DrawIngest;
import com.sunwinkr.lottery.engine.port.BetStore;
import com.sunwinkr.lottery.engine.port.ResultStore;
import com.sunwinkr.lottery.engine.port.SettledFlagStore;
import com.sunwinkr.lottery.engine.settle.LotterySettleService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * MVC slice for POST /api/v2/lottery/admin/xsmb/rescrape — dual-control
 * wrapper (audit §5.8). Requires secondApproverNickname != actor.
 */
@WebMvcTest(AdminLotteryController.class)
@Import(TestSecurityConfig.class)
class AdminLotteryRescrapeMvcTest {

    @Autowired MockMvc mvc;

    @MockBean HazelcastInstance hazelcast;
    @MockBean com.sunwinkr.lottery.api.security.RoleResolver roleResolver;
    @MockBean LotterySettleService settleService;
    @MockBean DrawIngest drawIngest;
    @MockBean ResultStore results;
    @MockBean SettledFlagStore settledFlag;
    @MockBean BetStore bets;
    @MockBean SettleAnnouncePublisher announcer;

    @Test
    @WithMockUser(username = "admin1", authorities = {"ROLE_PLAYER", "ROLE_LOTTERY_ADMIN"})
    void rescrape_missingSecondApprover_returns_400() throws Exception {
        String body = "{\"date\":\"2026-05-14\"}";
        mvc.perform(post("/api/v2/lottery/admin/xsmb/rescrape")
                .contentType(MediaType.APPLICATION_JSON).content(body))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    @WithMockUser(username = "admin1", authorities = {"ROLE_PLAYER", "ROLE_LOTTERY_ADMIN"})
    void rescrape_sameUserAsSecondApprover_returns_400() throws Exception {
        String body = "{\"date\":\"2026-05-14\",\"secondApproverNickname\":\"admin1\"}";
        mvc.perform(post("/api/v2/lottery/admin/xsmb/rescrape")
                .contentType(MediaType.APPLICATION_JSON).content(body))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    @WithMockUser(username = "admin1", authorities = {"ROLE_PLAYER", "ROLE_LOTTERY_ADMIN"})
    void rescrape_dualControlOk_runsIngest() throws Exception {
        when(drawIngest.runOnce(any(LocalDate.class)))
            .thenReturn(makeIngestSuccess());
        String body = "{\"date\":\"2026-05-14\",\"secondApproverNickname\":\"admin2\"}";
        mvc.perform(post("/api/v2/lottery/admin/xsmb/rescrape")
                .contentType(MediaType.APPLICATION_JSON).content(body))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true));
    }

    private static DrawIngest.IngestSummary makeIngestSuccess() {
        // Use reflection — IngestSummary has a private ctor; the noop()
        // factory returns scrapeFailed=false / counts=0 which is fine for
        // this test which just asserts on success path.
        return DrawIngest.IngestSummary.noop();
    }
}
