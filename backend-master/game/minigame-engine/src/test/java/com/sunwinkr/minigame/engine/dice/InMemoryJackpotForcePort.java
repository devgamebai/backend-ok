package com.sunwinkr.minigame.engine.dice;

import com.sunwinkr.minigame.engine.port.JackpotForcePort;

import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

/** Test fake — in-memory implementation of {@link JackpotForcePort}. */
public final class InMemoryJackpotForcePort implements JackpotForcePort {

    private final AtomicReference<Short> slot = new AtomicReference<>(null);

    @Override
    public Optional<Short> peekJackpotSide() {
        Short v = slot.getAndSet(null);
        return Optional.ofNullable(v);
    }

    public void set(short side) {
        slot.set(side);
    }
}
