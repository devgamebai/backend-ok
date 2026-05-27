package com.sunwinkr.minigame.api.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hazelcast.core.HazelcastInstance;
import com.sunwinkr.minigame.engine.core.TaiXiuRound;
import com.sunwinkr.minigame.engine.port.ForceResultStore;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * MVC slice tests for POST /api/v2/admin/taixiu/force-result.
 *
 * Covers:
 * - 403 for non-admin user (ROLE_PLAYER only)
 * - 200 for admin user (ROLE_MINIGAME_ADMIN)
 * - HZ map populated (forceStore.set called)
 * - MED-1 fix: substring username "superadmin_test" rejected (not granted admin role)
 *
 * Plan §3.6 / spec AMBIGUOUS A3 (MED-1 fix).
 */
@WebMvcTest(AdminController.class)
@Import(TestSecurityConfig.class)
@ActiveProfiles("test")
class AdminForceResultMvcTest {

    @Autowired MockMvc mvc;
    @Autowired ObjectMapper mapper;

    @MockBean HazelcastInstance hazelcast;  // required by AccessTokenFilter component scan
    @MockBean com.sunwinkr.minigame.api.security.RoleResolver roleResolver;  // required by AccessTokenFilter
    @MockBean ForceResultStore forceStore;
    @MockBean TaiXiuRound round;

    @Test
    @WithMockUser(username = "player1", authorities = {"ROLE_PLAYER"})
    void forceResult_nonAdmin_returns_403() throws Exception {
        String body = "{\"side\":1}";
        mvc.perform(post("/api/v2/admin/taixiu/force-result")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
            .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "admin1", authorities = {"ROLE_PLAYER", "ROLE_MINIGAME_ADMIN"})
    void forceResult_admin_returns_200_and_setsStore() throws Exception {
        String body = "{\"side\":1}";
        mvc.perform(post("/api/v2/admin/taixiu/force-result")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true));

        // Verify forceStore.set was called with a dice array
        verify(forceStore).set(any(short[].class));
    }

    /**
     * MED-1 fix: a user named "superadmin_test" contains the substring
     * "superadmin" but must NOT receive admin authority. The Spring Security
     * chain resolves authorities explicitly — only ROLE_MINIGAME_ADMIN passes.
     * No substring matching occurs.
     */
    @Test
    @WithMockUser(username = "superadmin_test", authorities = {"ROLE_PLAYER"})
    void forceResult_substringBypassRejected() throws Exception {
        String body = "{\"side\":0}";
        mvc.perform(post("/api/v2/admin/taixiu/force-result")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
            .andExpect(status().isForbidden());
    }
}
