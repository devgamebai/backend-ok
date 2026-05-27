package com.sunwinkr.minigame.api.controller.sicbo;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hazelcast.core.HazelcastInstance;
import com.sunwinkr.minigame.engine.core.RevealPhase;
import com.sunwinkr.minigame.engine.port.ForceResultStore;
import com.sunwinkr.minigame.engine.sicbo.core.SicboRound;
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
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * POST /api/v2/admin/sicbo/force-result — admin role gating + MED-1
 * substring-bypass verification for Sicbo.
 *
 * <p>Covers:
 *  - 403 for non-admin (ROLE_PLAYER only)
 *  - 200 for admin (ROLE_MINIGAME_ADMIN), forceStore.set called
 *  - MED-1 fix: substring username "superadmin_sicbo" without
 *    ROLE_MINIGAME_ADMIN is rejected with 403 (no substring shortcut)
 */
@WebMvcTest(AdminSicboController.class)
@Import(TestSicboSecurityConfig.class)
@ActiveProfiles("test")
class AdminSicboForceResultMvcTest {

    @Autowired MockMvc mvc;
    @Autowired ObjectMapper mapper;

    @MockBean HazelcastInstance hazelcast;
    @MockBean com.sunwinkr.minigame.api.security.RoleResolver roleResolver;
    @MockBean(name = "sicboForceResultStore") ForceResultStore forceStore;
    @MockBean SicboRound round;

    @Test
    @WithMockUser(username = "player1", authorities = {"ROLE_PLAYER"})
    void forceResult_nonAdmin_returns_403() throws Exception {
        String body = "{\"dice\":[1,2,3]}";
        mvc.perform(post("/api/v2/admin/sicbo/force-result")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
            .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "admin1", authorities = {"ROLE_PLAYER", "ROLE_MINIGAME_ADMIN"})
    void forceResult_admin_returns_200_and_setsStore() throws Exception {
        when(round.getReferenceId()).thenReturn(7L);
        when(round.getPhase()).thenReturn(RevealPhase.OPEN);

        String body = "{\"dice\":[4,5,6]}";
        mvc.perform(post("/api/v2/admin/sicbo/force-result")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true));

        // ketquataixiusicbo map populated via forceStore.set
        verify(forceStore).set(any(short[].class));
    }

    /**
     * MED-1 fix (Sicbo coverage): a user named "superadmin_sicbo"
     * contains the substring "superadmin" but must NOT receive admin
     * authority. The Spring Security chain resolves authorities
     * explicitly — only ROLE_MINIGAME_ADMIN passes. No substring
     * matching occurs.
     */
    @Test
    @WithMockUser(username = "superadmin_sicbo", authorities = {"ROLE_PLAYER"})
    void forceResult_substringBypassRejected() throws Exception {
        String body = "{\"dice\":[2,3,4]}";
        mvc.perform(post("/api/v2/admin/sicbo/force-result")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
            .andExpect(status().isForbidden());
    }
}
