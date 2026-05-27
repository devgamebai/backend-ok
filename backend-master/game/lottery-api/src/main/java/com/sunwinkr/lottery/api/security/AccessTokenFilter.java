package com.sunwinkr.lottery.api.security;

import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.IMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * Validates an {@code accessToken} on every protected request and binds
 * the resolved nickname + authorities into the Spring Security context.
 *
 * <h3>Token sources (in order)</h3>
 * <ol>
 *   <li>{@code Authorization: Bearer <token>} header</li>
 *   <li>{@code ?at=<token>} query param (legacy compatibility — same
 *       shape the BitZero clients already use)</li>
 * </ol>
 *
 * <h3>Resolution</h3>
 * {@code cacheToken} Hazelcast IMap maps {@code accessToken → nickname}.
 * If present, {@link RoleResolver#resolveAuthorities} produces the
 * authority list; the principal is the nickname string.
 *
 * <p>Mirror of TaiXiu PR-4
 * {@code com.sunwinkr.minigame.api.security.AccessTokenFilter}.
 *
 * <p>Anonymous requests pass through unauthenticated — the
 * {@link com.sunwinkr.lottery.api.config.SecurityConfig} chain decides
 * whether the endpoint requires auth.
 */
@Component
public class AccessTokenFilter extends OncePerRequestFilter {

    private static final Logger LOG = LoggerFactory.getLogger(AccessTokenFilter.class);

    private static final String HEADER_AUTH = "Authorization";
    private static final String BEARER_PREFIX = "Bearer ";
    private static final String QUERY_AT = "at";

    private final HazelcastInstance hazelcast;
    private final RoleResolver roleResolver;

    public AccessTokenFilter(HazelcastInstance hazelcast,
                              RoleResolver roleResolver) {
        this.hazelcast = hazelcast;
        this.roleResolver = roleResolver;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                     HttpServletResponse response,
                                     FilterChain chain) throws ServletException, IOException {
        String token = extractToken(request);
        if (token != null && !token.isEmpty()) {
            try {
                IMap<String, String> tokenMap = hazelcast.getMap("cacheToken");
                String nickname = tokenMap.get(token);
                if (nickname != null && !nickname.isEmpty()) {
                    UsernamePasswordAuthenticationToken auth =
                        new UsernamePasswordAuthenticationToken(
                            nickname,
                            null,
                            roleResolver.resolveAuthorities(nickname));
                    SecurityContextHolder.getContext().setAuthentication(auth);
                }
            } catch (Throwable t) {
                LOG.warn("AccessTokenFilter: cache lookup failed", t);
                // Fall through unauthenticated — endpoint role check rejects.
            }
        }
        chain.doFilter(request, response);
    }

    /** Pulls the bearer token from the Authorization header or the {@code at} query param. */
    static String extractToken(HttpServletRequest request) {
        String header = request.getHeader(HEADER_AUTH);
        if (header != null && header.startsWith(BEARER_PREFIX)) {
            return header.substring(BEARER_PREFIX.length()).trim();
        }
        String at = request.getParameter(QUERY_AT);
        if (at != null && !at.isEmpty()) {
            return at.trim();
        }
        return null;
    }
}
