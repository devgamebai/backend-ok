package com.sunwinkr.minigame.api.controller.sicbo;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Minimal Sicbo security config for @WebMvcTest slices.
 *
 * <p>Disables CSRF + delegates auth to {@code @WithMockUser}. Admin
 * endpoints still require ROLE_MINIGAME_ADMIN so authorization tests
 * exercise the substring-bypass MED-1 fix.
 */
@TestConfiguration
public class TestSicboSecurityConfig {

    @Bean
    public SecurityFilterChain testSicboSecurityFilterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            .formLogin(form -> form.disable())
            .httpBasic(basic -> basic.disable())
            .authorizeHttpRequests(reg -> reg
                .antMatchers("/api/v2/admin/sicbo/**")
                    .hasAuthority("ROLE_MINIGAME_ADMIN")
                .antMatchers("/api/v2/sicbo/**").authenticated()
                .anyRequest().permitAll());
        return http.build();
    }
}
