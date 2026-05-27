package com.sunwinkr.minigame.api.config;

import com.sunwinkr.minigame.api.adapter.CachePortAdapter;
import com.sunwinkr.minigame.api.adapter.HazelcastForceResultStore;
import com.sunwinkr.minigame.api.adapter.HazelcastJackpotForcePort;
import com.sunwinkr.minigame.api.adapter.JdbcWalletPort;
import com.sunwinkr.minigame.api.adapter.MongoBetRecorder;
import com.sunwinkr.minigame.api.adapter.MongoJackpotStatePort;
import com.sunwinkr.minigame.api.adapter.RmqHistoryPublisher;
import com.sunwinkr.minigame.api.adapter.RtpResolverPortAdapter;
import com.sunwinkr.minigame.engine.bet.BetAcceptor;
import com.sunwinkr.minigame.engine.bet.BetLedger;
import com.sunwinkr.minigame.engine.core.RevealClock;
import com.sunwinkr.minigame.engine.core.TaiXiuRound;
import com.sunwinkr.minigame.engine.dice.HouseEdgeDiceGenerator;
import com.sunwinkr.minigame.engine.dice.ResultPipeline;
import com.sunwinkr.minigame.engine.jackpot.JackpotPool;
import com.sunwinkr.minigame.engine.jackpot.JackpotTriggerPolicy;
import com.sunwinkr.minigame.engine.port.BetRecorder;
import com.sunwinkr.minigame.engine.port.CachePort;
import com.sunwinkr.minigame.engine.port.ForceResultStore;
import com.sunwinkr.minigame.engine.port.JackpotForcePort;
import com.sunwinkr.minigame.engine.port.JackpotStatePort;
import com.sunwinkr.minigame.engine.port.WalletPort;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Engine bean wiring. Builds the pure-Java engine collaborators
 * ({@link TaiXiuRound}, {@link BetAcceptor}, {@link ResultPipeline}, etc.)
 * and binds each engine port to its production adapter
 * ({@link JdbcWalletPort}, {@link HazelcastForceResultStore}, ...).
 *
 * <p>Per plan §1.3 the engine module has zero Spring imports — the wiring
 * lives here so the engine stays portable.
 *
 * <p>Default state: round starts at refId=1, OPEN phase. The BitZero
 * gameLoop continues to drive ticks in shadow mode; once
 * {@code MINIGAME_ENGINE_ENABLED=1} is flipped the bridge takes over.
 */
@Configuration
public class EngineConfig {

    @Bean
    public RevealClock revealClock() {
        return new RevealClock.SimpleRevealClock();
    }

    // CachePort bean comes from CachePortAdapter @Component directly —
    // wrapper removed to fix NoUniqueBeanDefinitionException (2 beans of type CachePort).

    // Port wrapper beans removed — each adapter is @Component and implements
    // its port directly. ResultPipeline below uses @Qualifier to pick TaiXiu's
    // ForceResultStore (HazelcastForceResultStore) vs Sicbo's variant.

    @Bean
    public BetLedger betLedger() {
        return new BetLedger();
    }

    @Bean
    public JackpotPool jackpotPool(JackpotStatePort statePort) {
        long initial = 0L;
        try {
            initial = statePort.read();
        } catch (Throwable t) {
            // Fall back to floor — pool ctor clamps to FLOOR_VIN.
        }
        return new JackpotPool(initial);
    }

    @Bean
    public JackpotTriggerPolicy jackpotTriggerPolicy(JackpotForcePort forcePort) {
        return new JackpotTriggerPolicy(forcePort);
    }

    @Bean
    public HouseEdgeDiceGenerator houseEdgeDiceGenerator(RtpResolverPortAdapter rtp) {
        return new HouseEdgeDiceGenerator(rtp);
    }

    @Bean
    public ResultPipeline resultPipeline(
            @org.springframework.beans.factory.annotation.Qualifier("hazelcastForceResultStore") ForceResultStore forceStore,
            HouseEdgeDiceGenerator houseEdge,
            JackpotTriggerPolicy triggerPolicy) {
        return new ResultPipeline(forceStore, houseEdge, triggerPolicy);
    }

    @Bean
    public TaiXiuRound taiXiuRound(RevealClock clock,
                                    CachePort cache,
                                    ResultPipeline pipeline,
                                    BetLedger ledger,
                                    JackpotPool jackpotPool,
                                    JackpotTriggerPolicy triggerPolicy) {
        TaiXiuRound round = new TaiXiuRound(clock, cache);
        round.wireResultPipeline(pipeline, ledger, jackpotPool, triggerPolicy);
        return round;
    }

    @Bean
    public BetAcceptor betAcceptor() {
        return new BetAcceptor();
    }

    /**
     * Optional RMQ adapter — Boot autowires it if present in the
     * classpath. Wired here for clarity (not strictly needed because
     * RmqHistoryPublisher is a @Component).
     */
    @Bean
    public RmqHistoryPublisher rmqHistoryPublisher() {
        return new RmqHistoryPublisher();
    }
}
