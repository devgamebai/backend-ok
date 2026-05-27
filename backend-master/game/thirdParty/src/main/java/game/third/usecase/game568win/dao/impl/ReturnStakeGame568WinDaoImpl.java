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
import game.third.usecase.game568win.dao.ReturnStakeGame568WinDao;
import game.third.usecase.game568win.entities.ReturnStakeGame568Win;
import game.third.usecase.game568win.entities.Status;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;
import org.bson.Document;
import org.bson.conversions.Bson;

public class ReturnStakeGame568WinDaoImpl
implements ReturnStakeGame568WinDao {
    final String COLLECTION_NAME = "returnStake_game568win";

    private Date convertToDate(LocalDateTime localDateTime) {
        Instant instant = localDateTime.atZone(ZoneId.systemDefault()).toInstant();
        return Date.from(instant);
    }

    @Override
    public boolean createReturnStake(ReturnStakeGame568Win settleGame568Win) {
        MongoDatabase db = MongoDBConnectionFactory.getDB();
        MongoCollection col = db.getCollection("returnStake_game568win");
        Document doc = new Document();
        doc.append("companyKey", (Object)settleGame568Win.getCompanyKey());
        doc.append("username", (Object)settleGame568Win.getUsername());
        doc.append("currentStake", (Object)settleGame568Win.getCurrentStake());
        doc.append("returnStakeTime", (Object)this.convertToDate(settleGame568Win.getReturnStakeTime()));
        doc.append("transferCode", (Object)settleGame568Win.getTransferCode());
        doc.append("transactionId", (Object)settleGame568Win.getTransactionId());
        doc.append("productType", (Object)settleGame568Win.getProductType());
        doc.append("gameType", (Object)settleGame568Win.getGameType());
        doc.append("created_at", (Object)new Date());
        col.insertOne((Object)doc);
        return true;
    }

    @Override
    public boolean updateReturnStake(String transferCode, String TransactionId, Status status) {
        MongoDatabase db = MongoDBConnectionFactory.getDB();
        MongoCollection col = db.getCollection("returnStake_game568win");
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
    public ReturnStakeGame568Win getReturnStake(String transferCode, String TransactionId) {
        MongoDatabase db = MongoDBConnectionFactory.getDB();
        MongoCollection col = db.getCollection("returnStake_game568win");
        Document conditions = new Document();
        conditions.put("transferCode", (Object)transferCode);
        conditions.put("transactionId", (Object)TransactionId);
        Document result = (Document)col.find((Bson)conditions).first();
        if (result == null) {
            return null;
        }
        ReturnStakeGame568Win returnStakeGame568Win = new ReturnStakeGame568Win();
        returnStakeGame568Win.setCompanyKey(result.getString((Object)"companyKey"));
        returnStakeGame568Win.setUsername(result.getString((Object)"username"));
        returnStakeGame568Win.setCurrentStake(result.getDouble((Object)"currentStake"));
        returnStakeGame568Win.setReturnStakeTime(result.getDate((Object)"returnStakeTime").toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime());
        returnStakeGame568Win.setTransferCode(result.getString((Object)"transferCode"));
        returnStakeGame568Win.setTransactionId(result.getString((Object)"transactionId"));
        returnStakeGame568Win.setProductType(result.getInteger((Object)"productType"));
        returnStakeGame568Win.setGameType(result.getInteger((Object)"gameType"));
        return returnStakeGame568Win;
    }
}

