package com.sunwinkr.lottery.api.config;

import com.sunwinkr.lottery.api.security.AccessTokenFilter;
import com.sunwinkr.lottery.api.security.RoleResolver;
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
 *   <li>{@code /ws/lottery/**}                  — public (handshake auth via STOMP CONNECT)
 *   <li>{@code /api/v2/lottery/products}        — public</li>
 *   <li>{@code /api/v2/lottery/admin/xsmb/**}   — role {@link RoleResolver#ROLE_LOTTERY_ADMIN}</li>
 *   <li>{@code /api/v2/lottery/**}              — authenticated (any role; bearer required)</li>
 *   <li>{@code /actuator/health}, /info         — public (for kubernetes probes)</li>
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
                .antMatchers("/ws/lottery/**").permitAll()
                .antMatchers("/api/v2/lottery/products").permitAll()
                .antMatchers("/actuator/health", "/actuator/info").permitAll()
                .antMatchers("/api/v2/lottery/admin/**")
                    .hasAuthority(RoleResolver.ROLE_LOTTERY_ADMIN)
                .antMatchers("/api/v2/lottery/**").authenticated()
                .anyRequest().permitAll())
            .addFilterBefore(tokenFilter, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }

    /**
     * CORS config — permissive for Cocos JS dev (localhost:7456) + staging
     * domains. Production should tighten to specific origins.
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
