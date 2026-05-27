package com.sunwinkr.lottery.api.security;

import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.IMap;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;

/**
 * Role lookup for the lottery API layer. Mirrors TaiXiu PR-4
 * {@code com.sunwinkr.minigame.api.security.RoleResolver} — admin status
 * resolved exclusively via the Hazelcast {@code admin_session} map
 * (populated by backend-api on successful {@code c=701} admin login).
 *
 * <h3>MED-1 fix</h3>
 * Substring matching ({@code user.getName().contains("superadmin")})
 * would let any nickname containing the word "superadmin" pass
 * (e.g. {@code superadmin_test}, {@code mysuperadminx}). The new path
 * requires the user to be present in the Hazelcast {@code admin_session}
 * map. No substring matching.
 *
 * <h3>Authority encoding</h3>
 * <ul>
 *   <li>Every authenticated player → {@code ROLE_PLAYER}</li>
 *   <li>{@code admin_session} contains nickname → {@code ROLE_LOTTERY_ADMIN}</li>
 * </ul>
 *
 * <p>Plan §5.1 access matrix. Hazelcast lookup failure fails closed
 * (returns false / non-admin).
 */
@Component
public class RoleResolver {

    /** Role granted to every authenticated player. */
    public static final String ROLE_PLAYER = "ROLE_PLAYER";

    /** Role granted to verified admins (settle/rescrape/admin search). */
    public static final String ROLE_LOTTERY_ADMIN = "ROLE_LOTTERY_ADMIN";

    private final HazelcastInstance hazelcast;

    public RoleResolver(HazelcastInstance hazelcast) {
        this.hazelcast = hazelcast;
    }

    /**
     * Resolve the authorities for the supplied nickname. Returns
     * {@code [ROLE_PLAYER, ROLE_LOTTERY_ADMIN]} for admins,
     * {@code [ROLE_PLAYER]} otherwise. Never returns an empty list for a
     * non-null nickname.
     *
     * @param nickname authenticated nickname (must be non-null)
     */
    public Collection<? extends GrantedAuthority> resolveAuthorities(String nickname) {
        if (nickname == null) {
            return Collections.emptyList();
        }
        if (isAdmin(nickname)) {
            return Arrays.asList(
                new SimpleGrantedAuthority(ROLE_PLAYER),
                new SimpleGrantedAuthority(ROLE_LOTTERY_ADMIN));
        }
        return Collections.singletonList(new SimpleGrantedAuthority(ROLE_PLAYER));
    }

    /**
     * MED-1 fix: this method MUST NOT substring-match. Admin status is
     * resolved exclusively via the Hazelcast {@code admin_session} map.
     */
    boolean isAdmin(String nickname) {
        if (nickname == null || nickname.isEmpty()) {
            return false;
        }
        try {
            IMap<String, ?> adminSession = hazelcast.getMap("admin_session");
            if (adminSession.containsKey(nickname)) {
                return true;
            }
        } catch (Throwable t) {
            // Hazelcast unavailable → deny by default (fail-closed).
        }
        return false;
    }
}
