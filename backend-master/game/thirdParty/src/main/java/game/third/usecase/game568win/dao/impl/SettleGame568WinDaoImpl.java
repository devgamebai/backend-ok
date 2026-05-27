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
import game.third.usecase.game568win.dao.SettleGame568WinDao;
import game.third.usecase.game568win.entities.SettleGame568Win;
import game.third.usecase.game568win.entities.Status;
import game.third.usecase.game568win.model.ExtraInfo;
import game.third.usecase.game568win.model.SeamlessGameExtraInfo;
import java.util.Date;
import org.bson.Document;
import org.bson.conversions.Bson;

public class SettleGame568WinDaoImpl
implements SettleGame568WinDao {
    final String COLLECTION_NAME = "settle_game568win";

    @Override
    public boolean createSettle(SettleGame568Win settleGame568Win) {
        MongoDatabase db = MongoDBConnectionFactory.getDB();
        MongoCollection col = db.getCollection("settle_game568win");
        Document doc = new Document();
        doc.append("transferCode", (Object)settleGame568Win.getTransferCode());
        doc.append("winLoss", (Object)settleGame568Win.getWinLoss());
        doc.append("resultType", (Object)settleGame568Win.getResultType());
        doc.append("resultTime", (Object)settleGame568Win.getResultTime());
        doc.append("commissionStake", (Object)settleGame568Win.getCommissionStake());
        doc.append("gameResult", (Object)settleGame568Win.getGameResult());
        doc.append("companyKey", (Object)settleGame568Win.getCompanyKey());
        doc.append("username", (Object)settleGame568Win.getUsername());
        doc.append("productType", (Object)settleGame568Win.getProductType());
        doc.append("gameType", (Object)settleGame568Win.getGameType());
        doc.append("gpid", (Object)settleGame568Win.getGpid());
        doc.append("isCashOut", (Object)settleGame568Win.isCashOut());
        doc.put("status", (Object)settleGame568Win.getStatus().name());
        doc.append("created_at", (Object)new Date());
        if (settleGame568Win.getExtraInfo() != null) {
            doc.append("extraInfo", (Object)settleGame568Win.getExtraInfo().toJson());
        }
        if (settleGame568Win.getSeamlessGameExtraInfo() != null) {
            doc.append("seamlessGameExtraInfo", (Object)settleGame568Win.getSeamlessGameExtraInfo().toJson());
        }
        col.insertOne((Object)doc);
        return true;
    }

    @Override
    public boolean updateStatus(String transferCode, Status status) {
        MongoDatabase db = MongoDBConnectionFactory.getDB();
        MongoCollection col = db.getCollection("settle_game568win");
        Document query = new Document("transferCode", (Object)transferCode);
        Document updateDoc = new Document();
        updateDoc.put("status", (Object)status.name());
        updateDoc.put("last_updated_at", (Object)new Date());
        Document update = new Document("$set", (Object)updateDoc);
        col.updateOne((Bson)query, (Bson)update);
        return true;
    }

    @Override
    public boolean updateWinLoss(String transferCode, Status status, double winLoss) {
        MongoDatabase db = MongoDBConnectionFactory.getDB();
        MongoCollection col = db.getCollection("settle_game568win");
        Document query = new Document("transferCode", (Object)transferCode);
        Document updateDoc = new Document();
        updateDoc.put("status", (Object)status.name());
        updateDoc.put("winLoss", (Object)winLoss);
        updateDoc.put("last_updated_at", (Object)new Date());
        Document update = new Document("$set", (Object)updateDoc);
        col.updateOne((Bson)query, (Bson)update);
        return true;
    }

    @Override
    public SettleGame568Win getSettle(String transferCode) {
        Document query;
        MongoDatabase db = MongoDBConnectionFactory.getDB();
        MongoCollection col = db.getCollection("settle_game568win");
        Document result = (Document)col.find((Bson)(query = new Document("transferCode", (Object)transferCode))).first();
        if (result == null) {
            return null;
        }
        SettleGame568Win settleGame568Win = new SettleGame568Win();
        settleGame568Win.setTransferCode(result.getString((Object)"transferCode"));
        settleGame568Win.setWinLoss(result.getDouble((Object)"winLoss"));
        settleGame568Win.setResultType(result.getInteger((Object)"resultType"));
        settleGame568Win.setResultTime(result.getString((Object)"resultTime"));
        settleGame568Win.setCommissionStake(result.getDouble((Object)"commissionStake"));
        settleGame568Win.setGameResult(result.getString((Object)"gameResult"));
        settleGame568Win.setCompanyKey(result.getString((Object)"companyKey"));
        settleGame568Win.setUsername(result.getString((Object)"username"));
        settleGame568Win.setProductType(result.getInteger((Object)"productType"));
        settleGame568Win.setGameType(result.getInteger((Object)"gameType"));
        settleGame568Win.setGpid(result.getInteger((Object)"gpid"));
        settleGame568Win.setCashOut(result.getBoolean((Object)"isCashOut"));
        settleGame568Win.setExtraInfo(ExtraInfo.fromJson(result.getString((Object)"extraInfo")));
        settleGame568Win.setStatus(Status.fromString(result.getString((Object)"status")));
        settleGame568Win.setSeamlessGameExtraInfo(SeamlessGameExtraInfo.fromJson(result.getString((Object)"seamlessGameExtraInfo")));
        return settleGame568Win;
    }
}

