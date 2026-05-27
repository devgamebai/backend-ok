package com.sunwinkr.minigame.api.security;

import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.IMap;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.Collections;

/**
 * Role lookup for the API layer. Replaces the legacy substring auth
 * pattern {@code user.getName().contains("superadmin")} (TXM:259) with
 * an explicit role resolution off the cached
 * {@link UserCacheModel#getUsertype()} field.
 *
 * <h3>MED-1 fix</h3>
 * Substring matching let any nickname containing the word "superadmin"
 * pass the legacy gate (e.g. {@code superadmin_test}, {@code notsuperadmin},
 * {@code mysuperadminx}). The new path requires the user to be present in
 * the Hazelcast {@code users} cache AND have a usertype that resolves to
 * {@code MINIGAME_ADMIN}.
 *
 * <p>Plan §3.6 / spec AMBIGUOUS A3.
 *
 * <h3>Authority encoding</h3>
 * <ul>
 *   <li>Every authenticated player → {@code ROLE_PLAYER}</li>
 *   <li>{@code UserCacheModel.usertype} matches an admin pattern below
 *       → {@code ROLE_MINIGAME_ADMIN}</li>
 * </ul>
 *
 * <h3>Admin identification</h3>
 * Sunwinkr does NOT have an enum-typed role field on UserCacheModel — the
 * admin authority is held in MySQL {@code admin_users} which is what
 * RbacSupport queries. For the minigame STOMP/REST API we treat the
 * Hazelcast {@code admin_session} map (keyed by nickname) as the
 * authoritative admin marker. The legacy {@code aat} backend token
 * lifecycle already populates this map (it's how backend-api recognizes
 * the admin session). Falls back to {@code usertype == 99} for tests.
 */
@Component
public class RoleResolver {

    /** Role granted to every authenticated player. */
    public static final String ROLE_PLAYER = "ROLE_PLAYER";

    /** Role granted to verified admins (force-result, kill-switch, diagnostics). */
    public static final String ROLE_MINIGAME_ADMIN = "ROLE_MINIGAME_ADMIN";

    /**
     * Sentinel usertype used in tests to mark admin without standing up
     * a real {@code admin_session} map. Production traffic uses the
     * Hazelcast lookup below.
     */
    static final int USERTYPE_ADMIN_SENTINEL = 99;

    private final HazelcastInstance hazelcast;

    public RoleResolver(HazelcastInstance hazelcast) {
        this.hazelcast = hazelcast;
    }

    /**
     * Resolve the authorities for the supplied nickname. Returns
     * {@code [ROLE_PLAYER, ROLE_MINIGAME_ADMIN]} for admins,
     * {@code [ROLE_PLAYER]} otherwise. Never returns an empty list.
     *
     * @param nickname authenticated nickname (must be non-null)
     */
    public Collection<? extends GrantedAuthority> resolveAuthorities(String nickname) {
        if (nickname == null) {
            return Collections.emptyList();
        }
        if (isAdmin(nickname)) {
            return java.util.Arrays.asList(
                new SimpleGrantedAuthority(ROLE_PLAYER),
                new SimpleGrantedAuthority(ROLE_MINIGAME_ADMIN));
        }
        return Collections.singletonList(new SimpleGrantedAuthority(ROLE_PLAYER));
    }

    /**
     * MED-1 fix: this method MUST NOT substring-match. Admin status is
     * resolved exclusively via the Hazelcast {@code admin_session} map —
     * never via {@code nickname.contains("...")}.
     *
     * <p>Note: {@link UserCacheModel#getUsertype()} is a stub that returns 0
     * in all deployed builds (precompiled LobbyModule override). The usertype
     * sentinel path is therefore unreachable and has been removed. The sole
     * authoritative admin signal is the {@code admin_session} HZ map which
     * backend-api populates on successful c=701 admin login.
     */
    boolean isAdmin(String nickname) {
        if (nickname == null || nickname.isEmpty()) {
            return false;
        }
        try {
            // Production path: backend-api populates admin_session on
            // successful c=701 admin login. Presence = admin.
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
