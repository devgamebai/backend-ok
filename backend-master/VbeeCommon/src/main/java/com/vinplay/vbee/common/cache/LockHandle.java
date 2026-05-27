package com.vinplay.vbee.common.cache;

/**
 * AutoCloseable lock release handle. {@link #close()} releases the lock
 * exactly once; subsequent calls are no-ops.
 *
 * <p>Constructed by {@link DistCache#acquireLock}. Never instantiated by
 * callers directly.
 */
public interface LockHandle extends AutoCloseable {
    /** Releases the lock. Safe to call from any thread; idempotent. */
    @Override
    void close();
}
