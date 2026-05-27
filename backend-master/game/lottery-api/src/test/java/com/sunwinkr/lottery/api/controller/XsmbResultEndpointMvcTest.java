package com.sunwinkr.lottery.api.controller;

import com.hazelcast.core.HazelcastInstance;
import com.sunwinkr.lottery.engine.bet.BetAcceptor;
import com.sunwinkr.lottery.engine.clock.LotteryClock;
import com.sunwinkr.lottery.engine.model.LotteryResult;
import com.sunwinkr.lottery.engine.port.BetStore;
import com.sunwinkr.lottery.engine.port.ResultStore;
import com.sunwinkr.lottery.engine.port.SettledFlagStore;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Arrays;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * MVC slice for GET /api/v2/lottery/xsmb/result/{date}.
 *
 * <p>Verifies the L-1 gate (audit invariant): if {@code settled_at IS NULL}
 * the endpoint returns 404 even when the underlying row exists. Only
 * after {@link SettledFlagStore#markSettled} runs does the row become
 * visible. Closes finding L-1 (pre-settle result reveal).
 */
@WebMvcTest(XsmbController.class)
@Import(TestSecurityConfig.class)
class XsmbResultEndpointMvcTest {

    @Autowired MockMvc mvc;

    @MockBean HazelcastInstance hazelcast;
    @MockBean com.sunwinkr.lottery.api.security.RoleResolver roleResolver;
    @MockBean BetAcceptor betAcceptor;
    @MockBean BetStore bets;
    @MockBean ResultStore results;
    @MockBean SettledFlagStore settledFlag;
    @MockBean Clock clock;

    /** Pin clock to 10:00 VN so result() can call clock.withZone(VN) safely. */
    @BeforeEach
    void wireClockMorning() {
        Clock fixed = Clock.fixed(
            LocalDateTime.of(2026, 5, 14, 10, 0).atZone(LotteryClock.VN).toInstant(),
            LotteryClock.VN);
        when(clock.withZone(any(ZoneId.class))).thenReturn(fixed);
        when(clock.instant()).thenReturn(fixed.instant());
        when(clock.getZone()).thenReturn(fixed.getZone());
    }

    /** Concrete: scrape persisted row with settled_at=NULL — endpoint 404s.
     *  Uses a PAST date (2026-05-13, one day before the pinned clock 2026-05-14)
     *  so the controller hits the "past date + not settled → DRAW_MISSING 404"
     *  branch rather than the "today + DRAW_PENDING → 200" branch.
     */
    @Test
    @WithMockUser(username = "player1", authorities = {"ROLE_PLAYER"})
    void result_preSettle_returns_404_even_if_row_exists() throws Exception {
        LocalDate target = LocalDate.parse("2026-05-13");
        // Pre-settle: SettledFlagStore.isSettled() == false (row has
        // settled_at IS NULL). Engine gate triggers BEFORE we even hit
        // the result store — so the result store would never be asked.
        when(settledFlag.isSettled(target)).thenReturn(false);
        // Even if the underlying row exists, the gate fires first.
        when(results.findByDate(target)).thenReturn(Optional.of(makeResult()));

        mvc.perform(get("/api/v2/lottery/xsmb/result/2026-05-13"))
            .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(username = "player1", authorities = {"ROLE_PLAYER"})
    void result_postSettle_returns_200_payload() throws Exception {
        LocalDate target = LocalDate.parse("2026-05-14");
        when(settledFlag.isSettled(target)).thenReturn(true);
        when(results.findByDate(target)).thenReturn(Optional.of(makeResult()));

        mvc.perform(get("/api/v2/lottery/xsmb/result/2026-05-14"))
            .andExpect(status().isOk());
    }

    private static LotteryResult makeResult() {
        LotteryResult r = new LotteryResult();
        r.setTime("14-05-2026");
        r.setCountNumbers(27);
        LotteryResult.Results res = new LotteryResult.Results();
        res.setĐB(Arrays.asList("12345"));
        res.setG1(Arrays.asList("99999"));
        r.setResults(res);
        return r;
    }
}
