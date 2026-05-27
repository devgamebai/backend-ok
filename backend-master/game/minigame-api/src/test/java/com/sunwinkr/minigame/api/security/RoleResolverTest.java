package com.sunwinkr.minigame.api.security;

import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.IMap;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.security.core.GrantedAuthority;

import java.util.Collection;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

/**
 * Unit tests for RoleResolver — covers MED-1 fix (no substring matching).
 *
 * <p>The sole admin signal is the Hazelcast {@code admin_session} map
 * (populated by backend-api on c=701 admin login). UserCacheModel.getUsertype()
 * stubs return 0 in all deployed builds so the usertype path has been removed.
 *
 * Plan §3.6 / spec AMBIGUOUS A3.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class RoleResolverTest {

    @Mock HazelcastInstance hazelcast;
    @Mock IMap<String, Object> adminSessionMap;

    @SuppressWarnings("unchecked")
    private RoleResolver newResolver() {
        when(hazelcast.getMap(eq("admin_session"))).thenReturn((IMap) adminSessionMap);
        return new RoleResolver(hazelcast);
    }

    @Test
    void regularPlayer_getsOnlyRolePlayer() {
        RoleResolver resolver = newResolver();
        when(adminSessionMap.containsKey("player1")).thenReturn(false);

        Collection<? extends GrantedAuthority> authorities = resolver.resolveAuthorities("player1");

        assertThat(authorities).extracting(GrantedAuthority::getAuthority)
            .containsExactly(RoleResolver.ROLE_PLAYER)
            .doesNotContain(RoleResolver.ROLE_MINIGAME_ADMIN);
    }

    @Test
    void adminSessionPresent_getsAdminRole() {
        RoleResolver resolver = newResolver();
        when(adminSessionMap.containsKey("admin1")).thenReturn(true);

        Collection<? extends GrantedAuthority> authorities = resolver.resolveAuthorities("admin1");

        assertThat(authorities).extracting(GrantedAuthority::getAuthority)
            .contains(RoleResolver.ROLE_MINIGAME_ADMIN);
    }

    /**
     * MED-1 fix: a user named "superadmin_test" contains the substring
     * "superadmin" but must NOT receive admin authority. The new RoleResolver
     * checks ONLY the Hazelcast admin_session map — never does substring matching.
     */
    @Test
    void med1Fix_substringUsername_doesNotGetAdminRole() {
        RoleResolver resolver = newResolver();
        when(adminSessionMap.containsKey("superadmin_test")).thenReturn(false);

        boolean isAdmin = resolver.isAdmin("superadmin_test");

        assertThat(isAdmin).isFalse();
    }

    @Test
    void hazelcastException_failsClosed_returnsNonAdmin() {
        // If HZ throws, resolver must fail closed (return false, not grant admin).
        when(hazelcast.getMap(eq("admin_session"))).thenThrow(new RuntimeException("HZ offline"));
        RoleResolver resolver = new RoleResolver(hazelcast);

        boolean isAdmin = resolver.isAdmin("admin1");

        assertThat(isAdmin).isFalse();
    }

    @Test
    void nullNickname_returnsEmptyAuthorities() {
        RoleResolver resolver = newResolver();
        Collection<? extends GrantedAuthority> authorities = resolver.resolveAuthorities(null);
        assertThat(authorities).isEmpty();
    }
}
