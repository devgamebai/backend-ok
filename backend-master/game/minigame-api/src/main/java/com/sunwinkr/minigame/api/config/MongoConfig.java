package com.sunwinkr.minigame.api.config;

import com.vinplay.vbee.common.mongodb.MongoDBConnectionFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Configuration;

import javax.annotation.PostConstruct;

/**
 * Initialise the legacy {@link MongoDBConnectionFactory} from
 * {@code /app/config/mongo.properties} at Spring boot. Without this,
 * {@code MongoDBConnectionFactory.getDB()} falls back to its hardcoded
 * {@code localhost:27017} default — which fails in the Spring container
 * (the Mongo host is {@code mongodb} on the Docker network).
 *
 * <p>SUN-1341 F1/F2 — the {@code LegacyTaixiuHistoryPort} / {@code
 * LegacySicboHistoryPort} adapters call {@code getDB()} to write
 * {@code log_taixiu} / {@code log_sicbo} docs for c=303 visibility.
 * Without init() the writes silently fail against localhost.
 *
 * <p>Mirror of {@link RmqConfig} and {@link HazelcastConfig} bootstrap pattern.
 */
@Configuration
public class MongoConfig {

    private static final Logger LOG = LoggerFactory.getLogger(MongoConfig.class);

    @PostConstruct
    public void init() {
        try {
            MongoDBConnectionFactory.init();
            LOG.info("MongoDBConnectionFactory initialised from /app/config/mongo.properties");
        } catch (Throwable t) {
            LOG.error("MongoDBConnectionFactory.init failed — legacy log_taixiu/log_sicbo writes will hit localhost defaults", t);
        }
    }
}
