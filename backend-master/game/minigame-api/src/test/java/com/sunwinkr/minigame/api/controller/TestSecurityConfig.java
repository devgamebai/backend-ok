package com.sunwinkr.minigame.api.controller;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Minimal security config for @WebMvcTest slices.
 * Replaces the production SecurityConfig (which requires HazelcastInstance
 * for AccessTokenFilter) with a test version that:
 * - Disables CSRF so MockMvc POST requests work
 * - Delegates auth to @WithMockUser / @WithAnonymousUser test annotations
 * - Admin endpoints still require ROLE_MINIGAME_ADMIN so authorization tests work
 */
@TestConfiguration
public class TestSecurityConfig {

    @Bean
    public SecurityFilterChain testSecurityFilterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            .formLogin(form -> form.disable())
            .httpBasic(basic -> basic.disable())
            .authorizeHttpRequests(reg -> reg
                .antMatchers("/api/v2/admin/taixiu/**")
                    .hasAuthority("ROLE_MINIGAME_ADMIN")
                .antMatchers("/api/v2/taixiu/**").authenticated()
                .anyRequest().permitAll());
        return http.build();
    }
}
