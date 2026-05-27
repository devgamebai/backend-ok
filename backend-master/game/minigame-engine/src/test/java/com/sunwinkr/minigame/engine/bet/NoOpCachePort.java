package com.sunwinkr.minigame.engine.bet;

import com.sunwinkr.minigame.engine.port.CachePort;

/** No-op {@link CachePort} for tests that don't care about cache writes. */
final class NoOpCachePort implements CachePort {
    @Override public void setAllowBetting(long refId, boolean v) { }
    @Override public void removeAllowBetting(long refId) { }
    @Override public void setCurrentReference(long refId) { }
}
