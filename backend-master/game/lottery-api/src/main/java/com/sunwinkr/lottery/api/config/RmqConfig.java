package com.sunwinkr.lottery.api.config;

import com.vinplay.vbee.common.rmq.RMQConnectionFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Configuration;

import javax.annotation.PostConstruct;

/**
 * Initialise the legacy {@link RMQConnectionFactory} static defaults from
 * env vars at Spring boot. Without this, publishes to {@code queue_payment}
 * and {@code queue_log_money} from {@link com.vinplay.vbee.common.rmq.RMQPublishTask}
 * use the hardcoded {@code localhost:5672 vinplay/vinplay@123} defaults
 * baked into {@code RMQConnectionFactory}, which fail in the Spring
 * container — surfaces upstream as
 * {@code UserServiceImpl.updateMoney} returning {@code "1031"}, which the
 * lottery {@code /bet} endpoint maps to {@code "Wallet rejected"} after
 * {@code compensateAtomicGate} reverses the MySQL debit.
 *
 * <p>Env vars (set by Docker Compose):
 * <ul>
 *   <li>{@code RABBITMQ_HOST}     — default {@code rabbitmq}</li>
 *   <li>{@code RABBITMQ_USER}     — default {@code sunwinkr_rmq}</li>
 *   <li>{@code RABBITMQ_PASSWORD} — default {@code vinplay@123}</li>
 *   <li>{@code RABBITMQ_PORT}     — default {@code 5672}</li>
 * </ul>
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

        // 5s timeouts so a transient RMQ blip can't fail a wallet publish.
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
