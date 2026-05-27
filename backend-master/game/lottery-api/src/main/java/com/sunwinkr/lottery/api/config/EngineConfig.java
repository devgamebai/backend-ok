package com.sunwinkr.lottery.api.config;

import com.google.gson.Gson;
import com.sunwinkr.lottery.api.adapter.JdbcBetStore;
import com.sunwinkr.lottery.api.adapter.JdbcLotteryWalletPort;
import com.sunwinkr.lottery.api.adapter.JdbcResultStore;
import com.sunwinkr.lottery.api.adapter.JdbcSettledFlagStore;
import com.sunwinkr.lottery.api.adapter.OkHttpScrapeClient;
import com.sunwinkr.lottery.api.adapter.TelegramAlertAdapter;
import com.sunwinkr.lottery.engine.bet.BetAcceptor;
import com.sunwinkr.lottery.engine.ingest.DrawIngest;
import com.sunwinkr.lottery.engine.model.LotteryResult;
import com.sunwinkr.lottery.engine.port.BetStore;
import com.sunwinkr.lottery.engine.port.ResultStore;
import com.sunwinkr.lottery.engine.port.ScrapeClient;
import com.sunwinkr.lottery.engine.port.SettledFlagStore;
import com.sunwinkr.lottery.engine.port.WalletPort;
import com.sunwinkr.lottery.engine.settle.LotterySettleService;
import com.sunwinkr.lottery.engine.settle.SettleFailureEvent;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;
import java.util.function.Consumer;

/**
 * Engine bean wiring. Binds each engine port to its production adapter
 * and constructs the {@link BetAcceptor}, {@link LotterySettleService},
 * {@link DrawIngest} engine collaborators.
 *
 * <p>Per plan §1.3 the engine module has zero Spring imports — wiring
 * lives here so the engine stays portable.
 *
 * <p>Mirror of {@code com.sunwinkr.minigame.api.config.EngineConfig}.
 */
@Configuration
public class EngineConfig {

    @Bean
    public Clock systemClock() {
        return Clock.systemUTC();
    }

    // Port beans removed — each Jdbc/OkHttp adapter is @Component and implements
    // its port interface directly. Wrappers caused NoUniqueBeanDefinitionException
    // at Spring boot (cf. minigame-api CachePort fix).

    // settleFailureSink wrapper removed — TelegramAlertAdapter is @Component
    // and implements Consumer<SettleFailureEvent> directly.

    @Bean
    public BetAcceptor betAcceptor(WalletPort wallet,
                                    BetStore bets,
                                    SettledFlagStore settledFlag,
                                    Clock clock) {
        return new BetAcceptor(wallet, bets, settledFlag, clock, "vin");
    }

    @Bean
    public LotterySettleService lotterySettleService(BetStore bets,
                                                      WalletPort wallet,
                                                      Consumer<SettleFailureEvent> failureSink) {
        return new LotterySettleService(bets, wallet, failureSink, "vin");
    }

    @Bean
    public DrawIngest.JsonRenderer drawJsonRenderer() {
        final Gson gson = new Gson();
        return new DrawIngest.JsonRenderer() {
            @Override
            public String render(LotteryResult draw) {
                return gson.toJson(draw);
            }
        };
    }

    @Bean
    public DrawIngest drawIngest(ScrapeClient scraper,
                                  ResultStore results,
                                  SettledFlagStore settledFlag,
                                  LotterySettleService settleSvc,
                                  DrawIngest.JsonRenderer jsonRenderer) {
        return new DrawIngest(scraper, results, settledFlag, settleSvc, jsonRenderer);
    }
}
