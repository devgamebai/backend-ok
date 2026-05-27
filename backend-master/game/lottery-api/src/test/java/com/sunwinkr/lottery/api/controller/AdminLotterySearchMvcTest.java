package com.sunwinkr.lottery.api.controller;

import com.hazelcast.core.HazelcastInstance;
import com.sunwinkr.lottery.api.push.SettleAnnouncePublisher;
import com.sunwinkr.lottery.engine.ingest.DrawIngest;
import com.sunwinkr.lottery.engine.port.BetStore;
import com.sunwinkr.lottery.engine.port.ResultStore;
import com.sunwinkr.lottery.engine.port.SettledFlagStore;
import com.sunwinkr.lottery.engine.settle.LotterySettleService;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * MVC slice for GET /api/v2/lottery/admin/transactions — H3 fix.
 *
 * <p><b>The critical test:</b> a classic SQL-injection payload
 * ({@code ' OR 1=1 --}) in the {@code nickname} filter is captured and
 * passed through verbatim to {@link BetStore#search} as an
 * {@link java.util.Optional#of(Object)} value — there is NO string
 * concatenation step where the payload could break out. The adapter
 * (JdbcBetStore) uses PreparedStatement {@code setString(...)} so it
 * lands in the parameterized {@code nick_name = ?} clause that returns
 * zero rows.
 */
@WebMvcTest(AdminLotteryController.class)
@Import(TestSecurityConfig.class)
class AdminLotterySearchMvcTest {

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
    void sqlInjectionRejected_payloadFlowsAsParameterizedBinding() throws Exception {
        when(bets.search(any(BetStore.SearchFilter.class))).thenReturn(Collections.emptyList());
        when(bets.count(any(BetStore.SearchFilter.class))).thenReturn(0L);

        String injection = "' OR 1=1 --";

        mvc.perform(get("/api/v2/lottery/admin/transactions")
                .param("nickname", injection))
            .andExpect(status().isOk());

        // Capture the filter handed to BetStore.search — assert the
        // injection payload flowed as an Optional<String> value, NOT a
        // string-interpolated SQL fragment.
        ArgumentCaptor<BetStore.SearchFilter> captor =
            ArgumentCaptor.forClass(BetStore.SearchFilter.class);
        verify(bets).search(captor.capture());
        BetStore.SearchFilter actual = captor.getValue();
        assertThat(actual.nickname).isPresent();
        assertThat(actual.nickname.get()).isEqualTo(injection);
        // The adapter (JdbcBetStore) binds nickname via setString → it
        // can never split the SQL. This test asserts the controller did
        // its part: passed the raw value as data, not code.
    }
}
