package com.sunwinkr.minigame.api.adapter.sicbo;

import com.mongodb.client.MongoCollection;
import com.sunwinkr.minigame.engine.port.BetRecorder;
import com.vinplay.vbee.common.mongodb.MongoDBConnectionFactory;
import com.vinplay.vbee.common.mongodb.MongoRetryHelper;
import org.bson.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Sicbo bet recorder writing per-bet history rows to Mongo
 * {@code user_bet_tai_xiu_sicbo} (SBR:435-446 / spec INV-20).
 *
 * <p>Distinct from TaiXiu's {@code MongoBetRecorder} which writes to the
 * {@code user_bet_tai_xiu} collection. Both adapters live in the same
 * Spring context — each engine path injects the {@code BetRecorder} bean
 * it needs by {@code @Qualifier}.
 *
 * <p>Per SUN-1xxx, write failures are surfaced via slf4j WARN — they do
 * not silently swallow. The engine treats this as fire-and-forget.
 *
 * <p>Plan §6 / B9 (Sicbo analog).
 */
@Component("sicboBetRecorder")
public class MongoSicboBetRecorder implements BetRecorder {

    private static final Logger LOG = LoggerFactory.getLogger(MongoSicboBetRecorder.class);

    private static final String COLLECTION = "user_bet_tai_xiu_sicbo";

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
            LOG.warn("MongoSicboBetRecorder.recordBet failed user=" + r.nickname
                + " ref=" + r.refId + " betValue=" + r.betValue, t);
        }
    }

    /**
     * Drop the per-round history collection on new-round transition.
     * Mirrors SBR:370. Wrapped in {@link MongoRetryHelper#run} for the
     * known mongo-driver pool-recovery quirk
     * (docs/INFRA_ISSUES_stale-mongo-pool-and-cicd-overrebuild.md).
     */
    public void clearForNewRound() {
        try {
            MongoRetryHelper.run(() ->
                MongoDBConnectionFactory.getDB().getCollection(COLLECTION).drop(),
                "sicbo.clearUserBetToDb");
        } catch (Throwable t) {
            LOG.warn("MongoSicboBetRecorder.clearForNewRound failed", t);
        }
    }
}
