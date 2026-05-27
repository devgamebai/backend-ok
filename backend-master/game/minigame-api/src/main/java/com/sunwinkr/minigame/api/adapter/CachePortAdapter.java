package com.sunwinkr.minigame.api.adapter;

import com.sunwinkr.minigame.engine.port.CachePort;
import com.vinplay.dal.service.CacheService;
import com.vinplay.dal.service.impl.CacheServiceImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

/**
 * Adapter wrapping the legacy {@link CacheService} for the advisory
 * {@code allow_betting_*} keys. Implementations MUST swallow exceptions
 * — the engine's source of truth for bet acceptance is the JVM
 * {@code enableBetting} boolean, not these cache keys (spec §1).
 *
 * <p>Plan §2.1 L7.
 *
 * <p>{@code @Primary}: resolves the NoUniqueBeanDefinitionException when
 * the EngineConfig {@code cachePort} wrapper @Bean coexists with this
 * @Component in the same ApplicationContext. The @Component registration
 * is the canonical bean; the @Bean wrapper is a no-op delegate.
 */
@Primary
@Component
public class CachePortAdapter implements CachePort {

    private static final Logger LOG = LoggerFactory.getLogger(CachePortAdapter.class);

    private static final String CURRENT_REF_KEY = "Tai_xiu_current_reference";

    private final CacheService cacheService;

    public CachePortAdapter() {
        this(new CacheServiceImpl());
    }

    /** Test seam. */
    public CachePortAdapter(CacheService cacheService) {
        this.cacheService = cacheService;
    }

    @Override
    public void setAllowBetting(long refId, boolean v) {
        try {
            cacheService.setValue("allow_betting_" + refId, v ? 1 : 0);
        } catch (Throwable t) {
            LOG.warn("CachePortAdapter.setAllowBetting failed refId=" + refId, t);
        }
    }

    @Override
    public void removeAllowBetting(long refId) {
        try {
            cacheService.setValue("allow_betting_" + refId, 0);
        } catch (Throwable t) {
            LOG.warn("CachePortAdapter.removeAllowBetting failed refId=" + refId, t);
        }
    }

    @Override
    public void setCurrentReference(long refId) {
        try {
            cacheService.setValue(CURRENT_REF_KEY, Long.toString(refId));
        } catch (Throwable t) {
            LOG.warn("CachePortAdapter.setCurrentReference failed refId=" + refId, t);
        }
    }
}
