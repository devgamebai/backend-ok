package com.sunwinkr.minigame.api.config;

import com.vinplay.vbee.common.rmq.RMQConnectionFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Configuration;

import javax.annotation.PostConstruct;

/**
 * Mirror of {@code com.sunwinkr.lottery.api.config.RmqConfig} — initialise
 * the legacy {@link RMQConnectionFactory} static defaults from env vars at
 * Spring boot. Required for {@code UserServiceImpl.updateMoney} to publish
 * to {@code queue_payment} / {@code queue_log_money} without throwing
 * {@code "1031"} → {@code compensateAtomicGate} → {@code "Wallet rejected"}
 * on every minigame bet.
 */
@Configuration
public class RmqConfig {

    private static final Logger LOG = LoggerFactory.getLogger(RmqConfig.class);

    @PostConstruct
    public void init() {
        String host = envOrDefault("RABBITMQ_HOST", "rabbitmq");
        String user = envOrDefault("RABBITMQ_USER", "sunwinkr_rmq");
        String pass = envOrDefault("RABBITMQ_PASSWORD", "vinplay@123");
        int port = parseIntOr(System.getenv("RABBITMQ_PORT"), 5672);

        RMQConnectionFactory.init(user, pass, host, port, 5000, 5000, 5000);
        LOG.info("RmqConnectionFactory initialised host={}:{} user={}", host, port, user);
    }

    private static String envOrDefault(String name, String def) {
        String v = System.getenv(name);
        return (v != null && !v.isEmpty()) ? v : def;
    }

    private static int parseIntOr(String s, int def) {
        if (s == null || s.isEmpty()) return def;
        try { return Integer.parseInt(s.trim()); } catch (NumberFormatException e) { return def; }
    }
}
