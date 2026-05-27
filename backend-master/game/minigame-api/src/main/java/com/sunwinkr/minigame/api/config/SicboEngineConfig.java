package com.sunwinkr.minigame.api.config;

import com.sunwinkr.minigame.api.adapter.RtpResolverPortAdapter;
import com.sunwinkr.minigame.api.adapter.sicbo.HazelcastSicboForceResultStore;
import com.sunwinkr.minigame.api.adapter.sicbo.RmqSicboHistoryPublisher;
import com.sunwinkr.minigame.engine.port.ForceResultStore;
import com.sunwinkr.minigame.engine.sicbo.bet.SicboBetService;
import com.sunwinkr.minigame.engine.sicbo.bet.SicboPotState;
import com.sunwinkr.minigame.engine.sicbo.bet.SicboTxIdGenerator;
import com.sunwinkr.minigame.engine.sicbo.core.SicboRound;
import com.sunwinkr.minigame.engine.sicbo.dice.DefaultFeatureFlagPort;
import com.sunwinkr.minigame.engine.sicbo.dice.FeatureFlagPort;
import com.sunwinkr.minigame.engine.sicbo.dice.RtpResolverPort;
import com.sunwinkr.minigame.engine.sicbo.dice.SicboDiceGenerator;
import com.sunwinkr.minigame.engine.sicbo.dice.SicboFundProtector;
import com.sunwinkr.minigame.engine.sicbo.dice.SicboRandomDiceGenerator;
import com.sunwinkr.minigame.engine.sicbo.dice.SicboRtpDiceGenerator;
import com.sunwinkr.minigame.engine.sicbo.prize.SicboPayoutCalculator;
import com.sunwinkr.minigame.engine.sicbo.prize.SicboPrizeCalculator;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Sicbo engine bean wiring. Builds the pure-Java Sicbo collaborators
 * ({@link SicboRound}, {@link SicboBetService}, {@link SicboPrizeCalculator},
 * etc.) and binds each engine port to its production adapter.
 *
 * <p>Per plan §1.3 the engine module has zero Spring imports — the
 * wiring lives here so the engine stays portable. Mirrors TaiXiu's
 * {@code EngineConfig} pattern.
 *
 * <p>Default state: round starts at refId=1, OPEN phase. The BitZero
 * gameLoop continues to drive ticks in shadow mode; once
 * {@code MINIGAME_ENGINE_ENABLED=1} is flipped the SicboModuleBridge
 * takes over.
 */
@Configuration
public class SicboEngineConfig {

    /** Initial monotonic reference id — overwritten by the bridge on first round-start. */
    private static final long INITIAL_REF_ID = 1L;

    @Bean
    public SicboRound sicboRound() {
        return new SicboRound(INITIAL_REF_ID);
    }

    @Bean
    public SicboPotState sicboPotState() {
        return new SicboPotState();
    }

    @Bean
    public SicboTxIdGenerator sicboTxIdGenerator(SicboRound round) {
        return new SicboTxIdGenerator(round.getReferenceId());
    }

    @Bean
    public SicboBetService sicboBetService() {
        return new SicboBetService();
    }

    @Bean
    public SicboPayoutCalculator sicboPayoutCalculator() {
        return new SicboPayoutCalculator();
    }

    /** Default 0% house tax — matches legacy SBR:1064 baseline. */
    private static final float DEFAULT_TAX_PCT = 0f;

    @Bean
    public SicboPrizeCalculator sicboPrizeCalculator(SicboPayoutCalculator payout) {
        return new SicboPrizeCalculator(DEFAULT_TAX_PCT, payout);
    }

    @Bean
    public FeatureFlagPort sicboFeatureFlagPort() {
        return DefaultFeatureFlagPort.ALWAYS_ON;
    }

    @Bean
    public RtpResolverPort sicboRtpResolverPort(RtpResolverPortAdapter shared) {
        // Adapter wrapping com.vinplay.vbee.common.rtp.RtpResolver — both
        // the engine.dice.RtpResolverPort and sicbo.dice.RtpResolverPort
        // share the same effectivePct contract; bridge via a thin lambda.
        return new RtpResolverPort() {
            @Override
            public double effectivePct(long userId, String gameId) {
                return shared.effectivePct(userId, gameId);
            }
            @Override
            public double effectivePct(String gameId) {
                return shared.effectivePct(gameId);
            }
        };
    }

    @Bean
    public SicboRandomDiceGenerator sicboRandomDiceGenerator() {
        return new SicboRandomDiceGenerator();
    }

    @Bean
    public SicboDiceGenerator sicboDiceGenerator(FeatureFlagPort flag,
                                                  RtpResolverPort rtp,
                                                  SicboPayoutCalculator payout) {
        return new SicboRtpDiceGenerator(flag, rtp, payout);
    }

    @Bean
    public SicboFundProtector sicboFundProtector(SicboRandomDiceGenerator random,
                                                  SicboPayoutCalculator payout) {
        return new SicboFundProtector(random, payout);
    }

    /**
     * Qualified Sicbo force-result store. The bean name
     * {@code sicboForceResultStore} disambiguates from TaiXiu's
     * {@code forceResultStore} bean inside {@link EngineConfig}.
     */
    // Wrapper removed — HazelcastSicboForceResultStore is @Component
    // registered as bean name "hazelcastSicboForceResultStore". Sicbo
    // injections explicitly @Qualifier that name to avoid type ambiguity
    // with the TaiXiu adapter.

    /** Sicbo RMQ history publisher. */
    @Bean
    public RmqSicboHistoryPublisher rmqSicboHistoryPublisher() {
        return new RmqSicboHistoryPublisher();
    }
}
