package com.sunwinkr.minigame.api.config;

import com.sunwinkr.minigame.api.security.AccessTokenFilter;
import com.sunwinkr.minigame.api.security.RoleResolver;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import java.util.Arrays;

/**
 * Spring Security filter chain.
 *
 * <p>Path matrix per plan §5.1:
 * <ul>
 *   <li>{@code /ws/minigame/**}                — public (handshake auth via STOMP CONNECT)
 *   <li>{@code /api/v2/products/**}            — public
 *   <li>{@code /api/v2/admin/taixiu/**}        — role {@link RoleResolver#ROLE_MINIGAME_ADMIN}
 *   <li>{@code /api/v2/taixiu/**}              — authenticated (any role)
 *   <li>{@code /api/v2/admin/sicbo/**}         — role {@link RoleResolver#ROLE_MINIGAME_ADMIN}
 *   <li>{@code /api/v2/sicbo/**}               — authenticated (any role)
 *   <li>{@code /actuator/health}, /info        — public (for kubernetes probes)
 * </ul>
 *
 * <p>Auth principal is resolved by {@link AccessTokenFilter}, which runs
 * before the username/password filter and binds the
 * {@code UsernamePasswordAuthenticationToken} into the context.
 *
 * <p>CSRF is disabled — this API is invoked from non-browser clients
 * (Cocos JS client + admin CMS tooling) carrying their own bearer tokens.
 */
@Configuration
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http,
                                                    AccessTokenFilter tokenFilter) throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            .formLogin(form -> form.disable())
            .httpBasic(basic -> basic.disable())
            .logout(logout -> logout.disable())
            .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(reg -> reg
                .antMatchers("/ws/minigame/**").permitAll()
                .antMatchers("/api/v2/products/**").permitAll()
                .antMatchers("/actuator/health", "/actuator/info").permitAll()
                .antMatchers("/api/v2/admin/taixiu/**")
                    .hasAuthority(RoleResolver.ROLE_MINIGAME_ADMIN)
                .antMatchers("/api/v2/admin/sicbo/**")
                    .hasAuthority(RoleResolver.ROLE_MINIGAME_ADMIN)
                .antMatchers("/api/v2/taixiu/**").authenticated()
                .antMatchers("/api/v2/sicbo/**").authenticated()
                .anyRequest().permitAll())
            .addFilterBefore(tokenFilter, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }

    /**
     * CORS — Cocos JS dev (localhost:*) + staging domains. Tighten for prod.
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration cfg = new CorsConfiguration();
        cfg.setAllowedOriginPatterns(Arrays.asList(
                "http://localhost:*",
                "https://*.sunkr.bet",
                "https://*.sunkr.club"));
        cfg.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH"));
        cfg.setAllowedHeaders(Arrays.asList("*"));
        cfg.setExposedHeaders(Arrays.asList("Authorization"));
        cfg.setAllowCredentials(true);
        cfg.setMaxAge(3600L);
        UrlBasedCorsConfigurationSource src = new UrlBasedCorsConfigurationSource();
        src.registerCorsConfiguration("/**", cfg);
        return src;
    }
}
