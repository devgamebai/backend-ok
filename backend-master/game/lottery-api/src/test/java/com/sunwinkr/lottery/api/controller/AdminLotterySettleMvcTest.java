package com.sunwinkr.lottery.api.controller;

import com.hazelcast.core.HazelcastInstance;
import com.sunwinkr.lottery.api.push.SettleAnnouncePublisher;
import com.sunwinkr.lottery.engine.ingest.DrawIngest;
import com.sunwinkr.lottery.engine.model.LotteryResult;
import com.sunwinkr.lottery.engine.port.BetStore;
import com.sunwinkr.lottery.engine.port.ResultStore;
import com.sunwinkr.lottery.engine.port.SettledFlagStore;
import com.sunwinkr.lottery.engine.settle.LotterySettleService;
import com.sunwinkr.lottery.engine.settle.SettleSummary;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * MVC slice for POST /api/v2/lottery/admin/xsmb/settle. Covers:
 *   - 403 for non-admin role
 *   - 200 for admin, fresh settle runs LotterySettleService
 *   - idempotent: second call on a settled day reports alreadySettled
 *
 * Plan §5.1 admin matrix, MED-1 fix (role-gated, no substring).
 */
@WebMvcTest(AdminLotteryController.class)
@Import(TestSecurityConfig.class)
class AdminLotterySettleMvcTest {

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
    @WithMockUser(username = "player1", authorities = {"ROLE_PLAYER"})
    void settle_nonAdmin_returns_403() throws Exception {
        String body = "{\"date\":\"2026-05-14\"}";
        mvc.perform(post("/api/v2/lottery/admin/xsmb/settle")
                .contentType(MediaType.APPLICATION_JSON).content(body))
            .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "admin1", authorities = {"ROLE_PLAYER", "ROLE_LOTTERY_ADMIN"})
    void settle_admin_freshDay_runsSettle_andAnnounces() throws Exception {
        LocalDate target = LocalDate.parse("2026-05-14");
        when(settledFlag.isSettled(target)).thenReturn(false);
        when(results.findByDate(target)).thenReturn(Optional.of(makeResult()));
        when(settleService.settleAll(any(LocalDate.class), any(LotteryResult.class)))
            .thenReturn(new SettleSummary(7, 0, 700000L));
        String body = "{\"date\":\"2026-05-14\"}";
        mvc.perform(post("/api/v2/lottery/admin/xsmb/settle")
                .contentType(MediaType.APPLICATION_JSON).content(body))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.alreadySettled").value(false))
            .andExpect(jsonPath("$.settledCount").value(7));
        verify(settledFlag).markSettled(target);
        verify(announcer).announceSettled(target);
    }

    /** Idempotency — second call returns existing count (no re-settle). */
    @Test
    @WithMockUser(username = "admin1", authorities = {"ROLE_PLAYER", "ROLE_LOTTERY_ADMIN"})
    void settle_admin_alreadySettled_returnsExistingCount() throws Exception {
        LocalDate target = LocalDate.parse("2026-05-14");
        when(settledFlag.isSettled(target)).thenReturn(true);
        when(bets.count(any(BetStore.SearchFilter.class))).thenReturn(7L);
        String body = "{\"date\":\"2026-05-14\"}";
        mvc.perform(post("/api/v2/lottery/admin/xsmb/settle")
                .contentType(MediaType.APPLICATION_JSON).content(body))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.alreadySettled").value(true))
            .andExpect(jsonPath("$.settledCount").value(7));
    }

    private static LotteryResult makeResult() {
        LotteryResult r = new LotteryResult();
        r.setTime("14-05-2026");
        r.setCountNumbers(27);
        r.setResults(new LotteryResult.Results());
        return r;
    }
}
