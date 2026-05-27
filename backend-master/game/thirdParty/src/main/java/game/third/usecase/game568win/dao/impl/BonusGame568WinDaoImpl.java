/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mongodb.client.MongoCollection
 *  com.mongodb.client.MongoDatabase
 *  com.vinplay.vbee.common.mongodb.MongoDBConnectionFactory
 *  org.bson.Document
 *  org.bson.conversions.Bson
 */
package game.third.usecase.game568win.dao.impl;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.vinplay.vbee.common.mongodb.MongoDBConnectionFactory;
import game.third.usecase.game568win.dao.BonusGame568WinDao;
import game.third.usecase.game568win.entities.BonusGame568Win;
import game.third.usecase.game568win.entities.Status;
import game.third.usecase.game568win.model.SeamlessGameExtraInfo;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;
import org.bson.Document;
import org.bson.conversions.Bson;

public class BonusGame568WinDaoImpl
implements BonusGame568WinDao {
    final String COLLECTION_NAME = "bonus_game568win";

    private Date convertToDate(LocalDateTime localDateTime) {
        Instant instant = localDateTime.atZone(ZoneId.systemDefault()).toInstant();
        return Date.from(instant);
    }

    @Override
    public boolean createBonus(BonusGame568Win bonusGame568Win) {
        MongoDatabase db = MongoDBConnectionFactory.getDB();
        MongoCollection col = db.getCollection("bonus_game568win");
        Document doc = new Document();
        doc.append("companyKey", (Object)bonusGame568Win.getCompanyKey());
        doc.append("username", (Object)bonusGame568Win.getUsername());
        doc.append("amount", (Object)bonusGame568Win.getAmount());
        doc.append("bonusTime", (Object)this.convertToDate(bonusGame568Win.getBonusTime()));
        doc.append("isGameProviderPromotion", (Object)bonusGame568Win.isGameProviderPromotion());
        doc.append("productType", (Object)bonusGame568Win.getProductType());
        doc.append("gameType", (Object)bonusGame568Win.getGameType());
        doc.append("transferCode", (Object)bonusGame568Win.getTransferCode());
        doc.append("transactionId", (Object)bonusGame568Win.getTransactionId());
        doc.append("gameId", (Object)bonusGame568Win.getGameId());
        doc.append("gpid", (Object)bonusGame568Win.getGpid());
        doc.append("created_at", (Object)new Date());
        if (bonusGame568Win.getSeamlessGameExtraInfo() != null) {
            doc.append("seamlessGameExtraInfo", (Object)bonusGame568Win.getSeamlessGameExtraInfo().toJson());
        }
        col.insertOne((Object)doc);
        return true;
    }

    @Override
    public boolean updateBonus(String transferCode, String TransactionId, Status status) {
        MongoDatabase db = MongoDBConnectionFactory.getDB();
        MongoCollection col = db.getCollection("bonus_game568win");
        Document conditions = new Document();
        conditions.put("transferCode", (Object)transferCode);
        conditions.put("transactionId", (Object)TransactionId);
        Document updateDoc = new Document();
        updateDoc.put("status", (Object)status.name());
        updateDoc.put("last_updated_at", (Object)new Date());
        Document update = new Document("$set", (Object)updateDoc);
        col.updateOne((Bson)conditions, (Bson)update);
        return true;
    }

    @Override
    public BonusGame568Win getBonus(String transferCode, String transactionId) {
        MongoDatabase db = MongoDBConnectionFactory.getDB();
        MongoCollection col = db.getCollection("bonus_game568win");
        Document conditions = new Document();
        conditions.put("transferCode", (Object)transferCode);
        conditions.put("transactionId", (Object)transactionId);
        Document result = (Document)col.find((Bson)conditions).first();
        if (result == null) {
            return null;
        }
        BonusGame568Win bonusGame568Win = new BonusGame568Win();
        bonusGame568Win.setCompanyKey(result.getString((Object)"companyKey"));
        bonusGame568Win.setUsername(result.getString((Object)"username"));
        bonusGame568Win.setAmount(result.getDouble((Object)"amount"));
        bonusGame568Win.setBonusTime(result.getDate((Object)"bonusTime").toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime());
        bonusGame568Win.setGameProviderPromotion(result.getBoolean((Object)"isGameProviderPromotion"));
        bonusGame568Win.setProductType(result.getInteger((Object)"productType"));
        bonusGame568Win.setGameType(result.getInteger((Object)"gameType"));
        bonusGame568Win.setTransferCode(result.getString((Object)"transferCode"));
        bonusGame568Win.setTransactionId(result.getString((Object)"transactionId"));
        bonusGame568Win.setGameId(result.getInteger((Object)"gameId"));
        bonusGame568Win.setGpid(result.getInteger((Object)"gpid"));
        bonusGame568Win.setSeamlessGameExtraInfo(SeamlessGameExtraInfo.fromJson(result.getString((Object)"seamlessGameExtraInfo")));
        return bonusGame568Win;
    }
}

