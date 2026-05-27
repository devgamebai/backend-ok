/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mongodb.client.MongoCollection
 *  com.mongodb.client.MongoDatabase
 *  com.mongodb.client.result.UpdateResult
 *  com.vinplay.vbee.common.mongodb.MongoDBConnectionFactory
 *  org.bson.Document
 *  org.bson.conversions.Bson
 */
package game.third.usecase.game568win.dao.impl;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.result.UpdateResult;
import com.vinplay.vbee.common.mongodb.MongoDBConnectionFactory;
import game.third.usecase.game568win.dao.TransactionGame568WinDAO;
import game.third.usecase.game568win.entities.Status;
import game.third.usecase.game568win.entities.TransactionGame568Win;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import org.bson.Document;
import org.bson.conversions.Bson;

public class TransactionGame568WinDAOImpl
implements TransactionGame568WinDAO {
    final String COLLECTION_NAME = "transaction_game568win";

    private Date convertToDate(LocalDateTime localDateTime) {
        Instant instant = localDateTime.atZone(ZoneId.systemDefault()).toInstant();
        return Date.from(instant);
    }

    @Override
    public boolean createTransaction(TransactionGame568Win transaction) {
        MongoDatabase db = MongoDBConnectionFactory.getDB();
        MongoCollection<Document> col = db.getCollection("transaction_game568win");
        Document doc = new Document();
        double winLoss = 0.0;
        doc.append("amount", transaction.getAmount()).append("transferCode", transaction.getTransferCode()).append("transactionId", transaction.getTransactionId()).append("betTime", this.convertToDate(transaction.getBetTime())).append("gameRoundId", transaction.getGameRoundId()).append("gamePeriodId", transaction.getGamePeriodId()).append("orderDetail", transaction.getOrderDetail()).append("playerIp", transaction.getPlayerIp()).append("gameTypeName", transaction.getGameTypeName()).append("companyKey", transaction.getCompanyKey()).append("username", transaction.getUsername()).append("productType", transaction.getProductType()).append("gameType", transaction.getGameType()).append("gameId", transaction.getGameId()).append("gpid", transaction.getGpid()).append("isGameProviderPromotion", transaction.isGameProviderPromotion()).append("winLoss", winLoss).append("created_at", new Date()).append("status", transaction.getStatus().name());
        if (transaction.getSeamlessGameExtraInfo() != null) {
            doc.append("seamlessGameExtraInfo", transaction.getSeamlessGameExtraInfo().toJson());
        }
        if (transaction.getExtraInfo() != null) {
            doc.append("extraInfo", transaction.getExtraInfo().toJson());
        }
        col.insertOne(doc);
        return true;
    }

    @Override
    public boolean updateSettleTransaction(String transferCode, String TransactionId, Status status, double winLoss, int ResultType, String ResultTime) {
        MongoDatabase db = MongoDBConnectionFactory.getDB();
        MongoCollection<Document> col = db.getCollection("transaction_game568win");
        Document conditions = new Document();
        conditions.put("transferCode", transferCode);
        conditions.put("transactionId", TransactionId);
        Document updateDoc = new Document();
        updateDoc.put("status", status.name());
        updateDoc.put("winLoss", winLoss);
        updateDoc.put("resultType", ResultType);
        updateDoc.put("resultTime", ResultTime);
        updateDoc.put("last_updated_at", new Date());
        Document update = new Document();
        update.put("$set", updateDoc);
        UpdateResult res = col.updateOne((Bson)conditions, (Bson)update);
        return res.getModifiedCount() > 0L;
    }

    @Override
    public boolean updateRollbackTransaction(String transferCode) {
        MongoDatabase db = MongoDBConnectionFactory.getDB();
        MongoCollection<Document> col = db.getCollection("transaction_game568win");
        Document conditions = new Document();
        conditions.put("transferCode", transferCode);
        double winLoss = 0.0;
        int resultType = 0;
        Document updateDoc = new Document();
        updateDoc.put("status", Status.Running.name());
        updateDoc.put("winLoss", winLoss);
        updateDoc.put("resultType", resultType);
        updateDoc.put("last_updated_at", new Date());
        Document update = new Document();
        update.put("$set", updateDoc);
        UpdateResult res = col.updateMany((Bson)conditions, (Bson)update);
        return res.getModifiedCount() > 0L;
    }

    @Override
    public boolean updateStatusTransaction(String transferCode, String TransactionId, Status status) {
        MongoDatabase db = MongoDBConnectionFactory.getDB();
        MongoCollection<Document> col = db.getCollection("transaction_game568win");
        Document conditions = new Document();
        conditions.put("transferCode", transferCode);
        conditions.put("transactionId", TransactionId);
        Document updateDoc = new Document();
        updateDoc.put("status", status.name());
        updateDoc.put("last_updated_at", new Date());
        Document update = new Document();
        update.put("$set", updateDoc);
        UpdateResult res = col.updateOne((Bson)conditions, (Bson)update);
        return res.getModifiedCount() > 0L;
    }

    @Override
    public boolean updateAmountTransaction(String transferCode, String TransactionId, double amount) {
        MongoDatabase db = MongoDBConnectionFactory.getDB();
        MongoCollection<Document> col = db.getCollection("transaction_game568win");
        Document conditions = new Document();
        conditions.put("transferCode", transferCode);
        conditions.put("transactionId", TransactionId);
        Document updateDoc = new Document();
        updateDoc.put("amount", amount);
        updateDoc.put("last_updated_at", new Date());
        Document update = new Document();
        update.put("$set", updateDoc);
        UpdateResult res = col.updateOne((Bson)conditions, (Bson)update);
        return res.getModifiedCount() > 0L;
    }

    @Override
    public boolean updateAmountTransaction(String transferCode, String TransactionId, double amount, Status status) {
        MongoDatabase db = MongoDBConnectionFactory.getDB();
        MongoCollection<Document> col = db.getCollection("transaction_game568win");
        Document conditions = new Document();
        conditions.put("transferCode", transferCode);
        conditions.put("transactionId", TransactionId);
        Document updateDoc = new Document();
        updateDoc.put("amount", amount);
        updateDoc.put("status", status.name());
        updateDoc.put("last_updated_at", new Date());
        Document update = new Document();
        update.put("$set", updateDoc);
        UpdateResult res = col.updateOne((Bson)conditions, (Bson)update);
        return res.getModifiedCount() > 0L;
    }

    @Override
    public boolean updateStatusTransaction(String transferCode, String TransactionId, Status status, double returnAmount) {
        MongoDatabase db = MongoDBConnectionFactory.getDB();
        MongoCollection<Document> col = db.getCollection("transaction_game568win");
        Document conditions = new Document();
        conditions.put("transferCode", transferCode);
        conditions.put("transactionId", TransactionId);
        Document updateDoc = new Document();
        updateDoc.put("status", status.name());
        updateDoc.put("returnAmount", returnAmount);
        updateDoc.put("last_updated_at", new Date());
        Document update = new Document();
        update.put("$set", updateDoc);
        UpdateResult res = col.updateOne((Bson)conditions, (Bson)update);
        return res.getModifiedCount() > 0L;
    }

    @Override
    public boolean updateStatusTransaction(String transferCode, Status status) {
        MongoDatabase db = MongoDBConnectionFactory.getDB();
        MongoCollection<Document> col = db.getCollection("transaction_game568win");
        Document conditions = new Document();
        conditions.put("transferCode", transferCode);
        Document updateDoc = new Document();
        updateDoc.put("status", status.name());
        updateDoc.put("last_updated_at", new Date());
        Document update = new Document();
        update.put("$set", updateDoc);
        UpdateResult res = col.updateMany((Bson)conditions, (Bson)update);
        return res.getModifiedCount() > 0L;
    }

    @Override
    public TransactionGame568Win getFirstTransactionById(String transactionCode) {
        Document query;
        MongoDatabase db = MongoDBConnectionFactory.getDB();
        MongoCollection<Document> col = db.getCollection("transaction_game568win");
        Document result = (Document)col.find((Bson)(query = new Document("transferCode", transactionCode))).first();
        if (result == null) {
            return null;
        }
        return this.mapDoc(result);
    }

    @Override
    public List<TransactionGame568Win> getTransactionById(String transactionCode) {
        ArrayList<TransactionGame568Win> transactionGame568WinList = new ArrayList<TransactionGame568Win>();
        MongoDatabase db = MongoDBConnectionFactory.getDB();
        MongoCollection<Document> col = db.getCollection("transaction_game568win");
        Document query = new Document("transferCode", transactionCode);
        for (Document result : col.find(query)) {
            TransactionGame568Win transactionGame568Win = this.mapDoc(result);
            transactionGame568WinList.add(transactionGame568Win);
        }
        return transactionGame568WinList;
    }

    private TransactionGame568Win mapDoc(Document result) {
        TransactionGame568Win transactionGame568Win = new TransactionGame568Win();
        transactionGame568Win.setAmount(result.getDouble("amount"));
        transactionGame568Win.setTransferCode(result.getString("transferCode"));
        transactionGame568Win.setTransactionId(result.getString("transactionId"));
        transactionGame568Win.setBetTime(result.getDate("betTime").toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime());
        transactionGame568Win.setGameRoundId(result.getString("gameRoundId"));
        transactionGame568Win.setGamePeriodId(result.getString("gamePeriodId"));
        transactionGame568Win.setOrderDetail(result.getString("orderDetail"));
        transactionGame568Win.setPlayerIp(result.getString("playerIp"));
        transactionGame568Win.setGameTypeName(result.getString("gameTypeName"));
        transactionGame568Win.setCompanyKey(result.getString("companyKey"));
        transactionGame568Win.setUsername(result.getString("username"));
        transactionGame568Win.setProductType(result.getInteger("productType"));
        transactionGame568Win.setGameType(result.getInteger("gameType"));
        transactionGame568Win.setGameId(result.getInteger("gameId"));
        transactionGame568Win.setGpid(result.getInteger("gpid"));
        transactionGame568Win.setWinLoss(result.getDouble("winLoss"));
        transactionGame568Win.setStatus(Status.fromString(result.getString("status")));
        transactionGame568Win.setGameProviderPromotion(result.getBoolean("isGameProviderPromotion"));
        return transactionGame568Win;
    }

    @Override
    public TransactionGame568Win getTransactionByTransferCodeAndTransactionId(String transferCode, String TransactionId) {
        Document query;
        MongoDatabase db = MongoDBConnectionFactory.getDB();
        MongoCollection<Document> col = db.getCollection("transaction_game568win");
        Document result = (Document)col.find((Bson)(query = new Document("transferCode", transferCode).append("transactionId", TransactionId))).first();
        if (result == null) {
            return null;
        }
        return this.mapDoc(result);
    }
}

