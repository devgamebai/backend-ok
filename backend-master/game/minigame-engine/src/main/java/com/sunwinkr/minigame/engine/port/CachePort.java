package com.sunwinkr.minigame.engine.port;

/**
 * Engine-facing port for the advisory Hazelcast {@code allow_betting_*}
 * cache. Per {@code docs/specs/taixiu-sicbo-rules-spec.md §1}, the
 * Hazelcast value is advisory only — the source of truth for bet
 * acceptance is the JVM {@code enableBetting} boolean held inside
 * {@code TaiXiuRound}. The cache exists so peer services (portal-api,
 * agency-cms diagnostics) can sample bet-window state without holding
 * a reference to the room.
 *
 * <p>Per plan §2.1 L7 (TXM:351-373), every write is wrapped in a
 * per-call try/catch by the adapter — propagation back into the engine
 * is forbidden so cache failures cannot break the round.
 *
 * <p>PR-1 scope: interface only. {@code HazelcastCachePort} adapter
 * lands in PR-3 alongside the BitZero bridge.
 */
public interface CachePort {

    /**
     * Write {@code allow_betting_<refId> = v} into the shared cache.
     * Implementations MUST swallow exceptions.
     *
     * @param refId reference id of the active round
     * @param v     {@code true} when bets are open, {@code false} after lock
     */
    void setAllowBetting(long refId, boolean v);

    /**
     * Remove {@code allow_betting_<refId>} from the cache. Invoked by the
     * round on finish (count=50) to mirror legacy
     * {@code MGRoomTaiXiu.finish()} (TXR:208-225). Implementations MUST
     * swallow exceptions.
     */
    void removeAllowBetting(long refId);

    /**
     * Publish the current active refId so peer services can dereference
     * it without inspecting the round directly.
     */
    void setCurrentReference(long refId);
}
