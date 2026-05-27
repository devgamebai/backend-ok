package com.sunwinkr.minigame.api.adapter;

import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.IMap;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for HazelcastForceResultStore.
 * Verifies atomic write + peekAndConsume removes the key (INV-3).
 *
 * Plan §4.2 / spec INV-3.
 */
@ExtendWith(MockitoExtension.class)
class HazelcastForceResultStoreTest {

    @Mock HazelcastInstance hazelcast;
    @Mock IMap<String, short[]> map;

    HazelcastForceResultStore store;

    @BeforeEach
    void setUp() {
        when(hazelcast.<String, short[]>getMap("ketquataixiu")).thenReturn(map);
        store = new HazelcastForceResultStore(hazelcast);
    }

    @Test
    void set_putsCorrectDiceArray() {
        short[] dice = {2, 3, 5};
        store.set(dice);
        verify(map).put("ketquataixiu", new short[]{2, 3, 5});
    }

    @Test
    void peekAndConsume_returnsValueAndRemovesKey() {
        short[] stored = {1, 4, 6};
        when(map.remove(anyString())).thenReturn(stored);

        Optional<short[]> result = store.peekAndConsume();

        assertThat(result).isPresent();
        assertThat(result.get()).containsExactly((short) 1, (short) 4, (short) 6);
        verify(map).remove("ketquataixiu");
    }

    @Test
    void peekAndConsume_whenEmpty_returnsEmpty() {
        when(map.remove(anyString())).thenReturn(null);

        Optional<short[]> result = store.peekAndConsume();

        assertThat(result).isEmpty();
    }

    @Test
    void peekAndConsume_onHazelcastException_returnsEmpty() {
        when(map.remove(anyString())).thenThrow(new RuntimeException("HZ offline"));

        Optional<short[]> result = store.peekAndConsume();

        assertThat(result).isEmpty();
    }
}
