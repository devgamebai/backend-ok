package com.sunwinkr.minigame.api.adapter;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.ReplaceOptions;
import com.sunwinkr.minigame.engine.port.JackpotStatePort;
import com.vinplay.vbee.common.mongodb.MongoDBConnectionFactory;
import org.bson.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

/**
 * Adapter for the {@code jackpot_tx} Mongo singleton document
 * (TXR.updateJpValue:1234, spec §6).
 *
 * <p>Document shape:
 * <pre>
 *   { _id: "singleton", jackpotTX: "&lt;long&gt;" }
 * </pre>
 *
 * <p>Plan §4 / J4.
 *
 * <p>{@code @Primary}: resolves NoUniqueBeanDefinitionException when the
 * stale compiled EngineConfig {@code jackpotStatePort} @Bean wrapper coexists
 * with this @Component. This is the canonical JackpotStatePort.
 */
@Primary
@Component
public class MongoJackpotStatePort implements JackpotStatePort {

    private static final Logger LOG = LoggerFactory.getLogger(MongoJackpotStatePort.class);

    private static final String COLLECTION = "jackpot_tx";
    private static final String FIELD = "jackpotTX";
    private static final String SINGLETON_ID = "singleton";

    @Override
    public long read() {
        try {
            MongoCollection<Document> coll =
                MongoDBConnectionFactory.getDB().getCollection(COLLECTION);
            Document doc = coll.find(Filters.eq("_id", SINGLETON_ID)).first();
            if (doc == null) {
                return 0L;
            }
            Object v = doc.get(FIELD);
            if (v instanceof Number) {
                return ((Number) v).longValue();
            }
            if (v instanceof String) {
                try {
                    return Long.parseLong((String) v);
                } catch (NumberFormatException nfe) {
                    LOG.warn("Bad jackpotTX string=" + v, nfe);
                    return 0L;
                }
            }
            return 0L;
        } catch (Throwable t) {
            LOG.warn("MongoJackpotStatePort.read failed", t);
            return 0L;
        }
    }

    @Override
    public void write(long jpAmount) {
        try {
            MongoCollection<Document> coll =
                MongoDBConnectionFactory.getDB().getCollection(COLLECTION);
            Document doc = new Document("_id", SINGLETON_ID).append(FIELD, Long.toString(jpAmount));
            coll.replaceOne(Filters.eq("_id", SINGLETON_ID), doc,
                new ReplaceOptions().upsert(true));
        } catch (Throwable t) {
            LOG.warn("MongoJackpotStatePort.write failed jp=" + jpAmount, t);
        }
    }
}
