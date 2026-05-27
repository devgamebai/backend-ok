package com.sunwinkr.minigame.api.adapter;

import com.mongodb.client.MongoCollection;
import com.sunwinkr.minigame.engine.port.BetRecorder;
import com.vinplay.vbee.common.mongodb.MongoDBConnectionFactory;
import com.vinplay.vbee.common.mongodb.MongoRetryHelper;
import org.bson.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

/**
 * Adapter writing per-bet history rows to Mongo {@code user_bet_tai_xiu}
 * (TXR:324, spec §6 / INV-20).
 *
 * <p>Per SUN-1xxx, write failures are surfaced via slf4j WARN — they do
 * not silently swallow. The engine treats this as fire-and-forget.
 *
 * <p>Plan §4.3.
 *
 * <p>{@code @Primary}: resolves NoUniqueBeanDefinitionException when the
 * stale compiled EngineConfig {@code betRecorder} @Bean wrapper coexists
 * with this @Component. This is the canonical BetRecorder.
 */
@Primary
@Component
public class MongoBetRecorder implements BetRecorder {

    private static final Logger LOG = LoggerFactory.getLogger(MongoBetRecorder.class);

    private static final String COLLECTION = "user_bet_tai_xiu";

    @Override
    public void recordBet(BetRecord r) {
        if (r == null) {
            throw new NullPointerException("BetRecord");
        }
        try {
            MongoCollection<Document> coll =
                MongoDBConnectionFactory.getDB().getCollection(COLLECTION);
            Document doc = new Document()
                .append("referentId", r.refId)
                .append("round_id", r.refId)       // SUN-1339: provider contract — settle groups by round_id
                .append("nick_name", r.nickname)
                .append("inputTime", (int) r.inputTime)
                .append("betSide", (int) r.betSide)
                .append("betValue", r.betValue)
                .append("balance", r.balance)
                .append("money_type", r.moneyType == 1 ? 1 : 2);
            coll.insertOne(doc);
        } catch (Throwable t) {
            LOG.warn("MongoBetRecorder.recordBet failed user=" + r.nickname
                + " ref=" + r.refId + " betValue=" + r.betValue, t);
        }
    }

    /**
     * Drop the per-round history collection on new-round transition.
     * Mirrors TXR:370. Wrapped in {@link MongoRetryHelper#run} for the
     * known mongo-driver pool-recovery quirk
     * (docs/INFRA_ISSUES_stale-mongo-pool-and-cicd-overrebuild.md).
     */
    public void clearForNewRound() {
        try {
            MongoRetryHelper.run(() ->
                MongoDBConnectionFactory.getDB().getCollection(COLLECTION).drop(),
                "taixiu.clearUserBetToDb");
        } catch (Throwable t) {
            LOG.warn("MongoBetRecorder.clearForNewRound failed", t);
        }
    }
}
