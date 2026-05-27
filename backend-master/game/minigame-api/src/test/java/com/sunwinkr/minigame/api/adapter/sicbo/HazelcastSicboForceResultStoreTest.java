package com.sunwinkr.minigame.api.adapter.sicbo;

import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.IMap;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Tests for {@link HazelcastSicboForceResultStore}. Verifies that
 * writes/peeks/consumes target the {@code ketquataixiusicbo} map (not
 * TaiXiu's {@code ketquataixiu}) and that {@code peekAndConsume} uses
 * an atomic {@code remove}.
 */
class HazelcastSicboForceResultStoreTest {

    @SuppressWarnings("unchecked")
    @Test
    void setWritesToSicboMap() {
        HazelcastInstance hz = mock(HazelcastInstance.class);
        IMap<String, short[]> map = (IMap<String, short[]>) mock(IMap.class);
        when(hz.<String, short[]>getMap("ketquataixiusicbo")).thenReturn(map);

        HazelcastSicboForceResultStore store = new HazelcastSicboForceResultStore(hz);
        store.set(new short[] { 3, 4, 5 });

        verify(map).put(eq("ketquataixiusicbo"), any(short[].class));
    }

    @SuppressWarnings("unchecked")
    @Test
    void peekAndConsumeUsesAtomicRemove() {
        HazelcastInstance hz = mock(HazelcastInstance.class);
        IMap<String, short[]> map = (IMap<String, short[]>) mock(IMap.class);
        when(hz.<String, short[]>getMap("ketquataixiusicbo")).thenReturn(map);
        when(map.remove("ketquataixiusicbo")).thenReturn(new short[] { 1, 2, 3 });

        HazelcastSicboForceResultStore store = new HazelcastSicboForceResultStore(hz);
        Optional<short[]> r = store.peekAndConsume();

        assertThat(r).isPresent();
        assertThat(r.get()).containsExactly((short) 1, (short) 2, (short) 3);
        verify(map).remove("ketquataixiusicbo");
    }

    @SuppressWarnings("unchecked")
    @Test
    void peekAndConsumeMissingKeyReturnsEmpty() {
        HazelcastInstance hz = mock(HazelcastInstance.class);
        IMap<String, short[]> map = (IMap<String, short[]>) mock(IMap.class);
        when(hz.<String, short[]>getMap("ketquataixiusicbo")).thenReturn(map);
        when(map.remove("ketquataixiusicbo")).thenReturn(null);

        HazelcastSicboForceResultStore store = new HazelcastSicboForceResultStore(hz);
        assertThat(store.peekAndConsume()).isEmpty();
    }
}
