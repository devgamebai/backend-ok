package com.sunwinkr.lottery.api.controller;

import com.sunwinkr.lottery.api.security.RoleResolver;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Minimal security config for @WebMvcTest slices. Replaces the
 * production SecurityConfig (which requires HazelcastInstance for
 * AccessTokenFilter) with a test version that delegates auth to
 * @WithMockUser. Admin endpoints still require ROLE_LOTTERY_ADMIN.
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
                .antMatchers("/api/v2/lottery/admin/**")
                    .hasAuthority(RoleResolver.ROLE_LOTTERY_ADMIN)
                .antMatchers("/api/v2/lottery/products").permitAll()
                .antMatchers("/api/v2/lottery/**").authenticated()
                .anyRequest().permitAll());
        return http.build();
    }
}
