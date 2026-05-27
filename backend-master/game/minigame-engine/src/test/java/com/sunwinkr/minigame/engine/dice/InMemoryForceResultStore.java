package com.sunwinkr.minigame.engine.dice;

import com.sunwinkr.minigame.engine.port.ForceResultStore;

import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

/** Test fake — in-memory implementation of {@link ForceResultStore}. */
public final class InMemoryForceResultStore implements ForceResultStore {

    private final AtomicReference<short[]> slot = new AtomicReference<>(null);
    private final AtomicInteger peekCount = new AtomicInteger();

    @Override
    public Optional<short[]> peekAndConsume() {
        peekCount.incrementAndGet();
        short[] dice = slot.getAndSet(null);
        return Optional.ofNullable(dice);
    }

    @Override
    public void set(short[] dice) {
        slot.set(dice);
    }

    public int peekCount() {
        return peekCount.get();
    }

    public boolean hasValue() {
        return slot.get() != null;
    }
}
