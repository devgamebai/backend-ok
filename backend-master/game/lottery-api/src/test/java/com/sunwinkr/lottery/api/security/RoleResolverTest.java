package com.sunwinkr.lottery.api.security;

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
 * MED-1 fix coverage — substring-username "superadmin_test" MUST NOT
 * receive admin authority. Sole admin signal is the Hazelcast
 * {@code admin_session} map. Hazelcast failure fails closed.
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
        RoleResolver r = newResolver();
        when(adminSessionMap.containsKey("player1")).thenReturn(false);
        Collection<? extends GrantedAuthority> auths = r.resolveAuthorities("player1");
        assertThat(auths).extracting(GrantedAuthority::getAuthority)
            .containsExactly(RoleResolver.ROLE_PLAYER)
            .doesNotContain(RoleResolver.ROLE_LOTTERY_ADMIN);
    }

    @Test
    void adminSessionPresent_getsAdminRole() {
        RoleResolver r = newResolver();
        when(adminSessionMap.containsKey("admin1")).thenReturn(true);
        assertThat(r.resolveAuthorities("admin1"))
            .extracting(GrantedAuthority::getAuthority)
            .contains(RoleResolver.ROLE_LOTTERY_ADMIN);
    }

    @Test
    void med1Fix_substringUsername_doesNotGetAdminRole() {
        RoleResolver r = newResolver();
        when(adminSessionMap.containsKey("superadmin_test")).thenReturn(false);
        assertThat(r.isAdmin("superadmin_test")).isFalse();
    }

    @Test
    void hazelcastException_failsClosed_returnsNonAdmin() {
        when(hazelcast.getMap(eq("admin_session"))).thenThrow(new RuntimeException("HZ offline"));
        RoleResolver r = new RoleResolver(hazelcast);
        assertThat(r.isAdmin("admin1")).isFalse();
    }
}
