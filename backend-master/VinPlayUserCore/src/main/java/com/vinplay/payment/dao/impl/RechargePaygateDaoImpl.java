/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mongodb.BasicDBObject
 *  com.mongodb.Block
 *  com.mongodb.client.AggregateIterable
 *  com.mongodb.client.FindIterable
 *  com.mongodb.client.MongoCollection
 *  com.mongodb.client.MongoDatabase
 *  com.mongodb.client.model.Projections
 *  com.mongodb.util.JSON
 *  org.apache.commons.lang.StringUtils
 *  org.apache.log4j.Logger
 *  org.bson.Document
 *  org.bson.conversions.Bson
 *  org.bson.types.ObjectId
 */
package com.vinplay.payment.dao.impl;

import com.mongodb.BasicDBObject;
import com.mongodb.Block;
import com.mongodb.client.AggregateIterable;
import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Projections;
import com.mongodb.util.JSON;
import com.vinplay.payment.dao.RechargePaygateDao;
import com.vinplay.payment.entities.Bank;
import com.vinplay.payment.entities.DepositPaygateModel;
import com.vinplay.payment.entities.DepositPaygateReponse;
import com.vinplay.payment.utils.PayCommon;
import com.vinplay.usercore.utils.GameCommon;
import com.vinplay.vbee.common.mongodb.MongoDBConnectionFactory;
import com.vinplay.vbee.common.utils.DateTimeUtils;
import com.vinplay.vbee.common.utils.VinPlayUtils;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.bson.Document;
import org.bson.conversions.Bson;
import org.bson.types.ObjectId;

public class RechargePaygateDaoImpl
implements RechargePaygateDao {
    public static final Logger logger = Logger.getLogger(RechargePaygateDaoImpl.class);
    private static final String DEPOSIT_COLLECTION = "deposit_paygate";

    @Override
    public boolean CheckPending(String nickname, String providerName) {
        return false;
    }

    @Override
    public boolean isExistDeposit(String nickname) {
        try {
            MongoDatabase db = MongoDBConnectionFactory.getDBSlave();
            Document conditions = new Document();
            conditions.put("Nickname", nickname);
            conditions.put("Status", PayCommon.PAYSTATUS.COMPLETED.getId());
            long count = db.getCollection(DEPOSIT_COLLECTION).count((Bson)conditions);
            return count > 0L;
        }
        catch (Exception e) {
            logger.error(e);
            return false;
        }
    }

    @Override
    public boolean updatePendingStatusToFailedAfterMinus(int minutes, String provider) {
        try {
            MongoDatabase db = MongoDBConnectionFactory.getDB();
            Document conditions = new Document();
            BasicDBObject obj = new BasicDBObject();
            String currenTime = DateTimeUtils.getCurrentTime("yyyy-MM-dd HH:mm:ss");
            String dateTimeAfterMinus = DateTimeUtils.getTimeSpace(currenTime, "yyyy-MM-dd HH:mm:ss", minutes * -1, "mm");
            obj.put("$lte", dateTimeAfterMinus);
            conditions.put("ModifiedAt", obj);
            conditions.put("Status", PayCommon.PAYSTATUS.PENDING.getId());
            if (!"all".equals(provider)) {
                conditions.put("ProviderName", provider);
            }
            BasicDBObject updateFields = new BasicDBObject();
            updateFields.append("Status", PayCommon.PAYSTATUS.FAILED.getId());
            updateFields.append("Description", "Y\u00eau c\u1ea7u n\u1ea1p ti\u1ec1n b\u1ecb qu\u00e1 h\u1ea1n (>60 ph\u00fat)");
            updateFields.append("UserApprove", "AUTOMATION");
            updateFields.append("ModifiedAt", VinPlayUtils.getCurrentDateTime());
            db.getCollection(DEPOSIT_COLLECTION).updateMany((Bson)conditions, (Bson)new Document("$set", updateFields));
            return true;
        }
        catch (Exception e) {
            logger.error(e);
            return false;
        }
    }

    @Override
    public long Add(DepositPaygateModel depositPayWellModel) {
        try {
            MongoDatabase db = MongoDBConnectionFactory.getDB();
            MongoCollection col = db.getCollection(DEPOSIT_COLLECTION);
            Document doc = new Document();
            long Id = VinPlayUtils.generateTransId();
            doc.append("Id", String.valueOf(Id));
            doc.append("CreatedAt", VinPlayUtils.getCurrentDateTime());
            doc.append("ModifiedAt", VinPlayUtils.getCurrentDateTime());
            doc.append("IsDeleted", false);
            doc.append("CartId", String.valueOf(Id));
            doc.append("ReferenceId", "");
            doc.append("UserId", depositPayWellModel.UserId);
            doc.append("Username", depositPayWellModel.Username);
            doc.append("Nickname", depositPayWellModel.Nickname);
            doc.append("RequestTime", depositPayWellModel.RequestTime);
            doc.append("BankCode", depositPayWellModel.BankCode);
            doc.append("BankName", depositPayWellModel.BankName);
            doc.append("MerchantCode", depositPayWellModel.MerchantCode);
            doc.append("PaymentType", depositPayWellModel.getPayTypeStr());
            doc.append("ProviderName", depositPayWellModel.ProviderName);
            doc.append("Amount", depositPayWellModel.Amount);
            doc.append("AmountFee", depositPayWellModel.AmountFee);
            doc.append("Status", PayCommon.PAYSTATUS.PENDING.getId());
            doc.append("BankAccountNumber", depositPayWellModel.BankAccountNumber);
            doc.append("BankAccountName", depositPayWellModel.BankAccountName);
            doc.append("Description", depositPayWellModel.Description);
            doc.append("UserApprove", depositPayWellModel.UserApprove);
            col.insertOne(doc);
            return Id;
        }
        catch (Exception e) {
            return 0L;
        }
    }

    @Override
    public long topupByCash(DepositPaygateModel depositPayWellModel) {
        try {
            MongoDatabase db = MongoDBConnectionFactory.getDB();
            MongoCollection col = db.getCollection(DEPOSIT_COLLECTION);
            Document doc = new Document();
            long Id = VinPlayUtils.generateTransId();
            doc.append("Id", String.valueOf(Id));
            doc.append("CreatedAt", VinPlayUtils.getCurrentDateTime());
            doc.append("ModifiedAt", VinPlayUtils.getCurrentDateTime());
            doc.append("IsDeleted", false);
            doc.append("CartId", depositPayWellModel.CartId);
            doc.append("ReferenceId", depositPayWellModel.ReferenceId);
            doc.append("UserId", depositPayWellModel.UserId);
            doc.append("Username", depositPayWellModel.Username);
            doc.append("Nickname", depositPayWellModel.Nickname);
            doc.append("RequestTime", depositPayWellModel.RequestTime);
            doc.append("BankCode", depositPayWellModel.BankCode);
            doc.append("BankName", depositPayWellModel.BankName);
            doc.append("MerchantCode", depositPayWellModel.MerchantCode);
            doc.append("PaymentType", depositPayWellModel.getPayTypeStr());
            doc.append("ProviderName", depositPayWellModel.ProviderName);
            doc.append("Amount", depositPayWellModel.Amount);
            doc.append("AmountFee", 0L);
            doc.append("Status", PayCommon.PAYSTATUS.COMPLETED.getId());
            doc.append("BankAccountNumber", depositPayWellModel.BankAccountNumber);
            doc.append("BankAccountName", depositPayWellModel.BankAccountName);
            doc.append("Description", depositPayWellModel.Description);
            doc.append("UserApprove", depositPayWellModel.UserApprove);
            doc.append("AgentBankCode", depositPayWellModel.AgentBankCode);
            doc.append("AgentBankAccountName", depositPayWellModel.AgentBankAccountName);
            doc.append("AgentBankAccountNumber", depositPayWellModel.AgentBankAccountNumber);
            doc.append("Content", depositPayWellModel.Content);
            col.insertOne(doc);
            return Id;
        }
        catch (Exception e) {
            return 0L;
        }
    }

    @Override
    public Boolean Update(DepositPaygateModel depositPayWellModel) {
        try {
            MongoDatabase db = MongoDBConnectionFactory.getDB();
            Document doc = new Document();
            doc.append("Id", depositPayWellModel.Id);
            doc.append("CreatedAt", depositPayWellModel.CreatedAt);
            doc.append("ModifiedAt", VinPlayUtils.getCurrentDateTime());
            doc.append("IsDeleted", depositPayWellModel.IsDeleted);
            doc.append("CartId", depositPayWellModel.CartId);
            doc.append("ReferenceId", depositPayWellModel.ReferenceId);
            doc.append("UserId", depositPayWellModel.UserId);
            doc.append("Username", depositPayWellModel.Username);
            doc.append("Nickname", depositPayWellModel.Nickname);
            doc.append("RequestTime", depositPayWellModel.RequestTime);
            doc.append("BankCode", depositPayWellModel.BankCode);
            doc.append("BankName", depositPayWellModel.BankName);
            doc.append("MerchantCode", depositPayWellModel.MerchantCode);
            doc.append("PaymentType", depositPayWellModel.getPayTypeStr());
            doc.append("ProviderName", depositPayWellModel.ProviderName);
            doc.append("Amount", depositPayWellModel.Amount);
            doc.append("AmountFee", depositPayWellModel.AmountFee);
            doc.append("Status", depositPayWellModel.Status);
            doc.append("BankAccountNumber", depositPayWellModel.BankAccountNumber);
            doc.append("BankAccountName", depositPayWellModel.BankAccountName);
            doc.append("Description", depositPayWellModel.Description);
            doc.append("UserApprove", depositPayWellModel.UserApprove);
            db.getCollection(DEPOSIT_COLLECTION).updateOne((Bson)new Document("Id", depositPayWellModel.Id), (Bson)new Document("$set", doc));
            return true;
        }
        catch (Exception e) {
            return false;
        }
    }

    @Override
    public Boolean Delete(String id) {
        try {
            MongoDatabase db = MongoDBConnectionFactory.getDB();
            db.getCollection(DEPOSIT_COLLECTION).deleteOne((Bson)new Document("Id", id));
            return true;
        }
        catch (Exception e) {
            return false;
        }
    }

    @Override
    public Boolean UpdateStatus(String orderId, String referenceId, int status, String userApprove) {
        try {
            MongoDatabase db = MongoDBConnectionFactory.getDB();
            BasicDBObject updateFields = new BasicDBObject();
            updateFields.append("ReferenceId", referenceId);
            updateFields.append("Status", status);
            updateFields.append("ModifiedAt", VinPlayUtils.getCurrentDateTime());
            updateFields.append("UserApprove", userApprove);
            db.getCollection(DEPOSIT_COLLECTION).updateOne((Bson)new Document("CartId", orderId), (Bson)new Document("$set", updateFields));
            return true;
        }
        catch (Exception e) {
            return false;
        }
    }

    @Override
    public Boolean UpdateStatus(String orderId, String referenceId, int status, String userApprove, String reason) {
        try {
            MongoDatabase db = MongoDBConnectionFactory.getDB();
            BasicDBObject updateFields = new BasicDBObject();
            updateFields.append("ReferenceId", referenceId);
            updateFields.append("Status", status);
            updateFields.append("ModifiedAt", VinPlayUtils.getCurrentDateTime());
            updateFields.append("UserApprove", userApprove);
            updateFields.append("Description", reason);
            db.getCollection(DEPOSIT_COLLECTION).updateOne((Bson)new Document("CartId", orderId), (Bson)new Document("$set", updateFields));
            return true;
        }
        catch (Exception e) {
            return false;
        }
    }

    @Override
    public Boolean delete(String id) {
        try {
            MongoDatabase db = MongoDBConnectionFactory.getDB();
            db.getCollection(DEPOSIT_COLLECTION).deleteOne((Bson)new Document("Id", id));
            return true;
        }
        catch (Exception e) {
            return false;
        }
    }

    @Override
    public Boolean UpdateStatus(String orderId, int status, String userApprove) {
        try {
            MongoDatabase db = MongoDBConnectionFactory.getDB();
            BasicDBObject updateFields = new BasicDBObject();
            updateFields.append("Status", status);
            updateFields.append("ModifiedAt", VinPlayUtils.getCurrentDateTime());
            updateFields.append("UserApprove", userApprove);
            db.getCollection(DEPOSIT_COLLECTION).updateOne((Bson)new Document("CartId", orderId), (Bson)new Document("$set", updateFields));
            return true;
        }
        catch (Exception e) {
            return false;
        }
    }

    @Override
    public Boolean UpdateRequestTime(String orderId, String reRequestTime, String userApprove) {
        try {
            MongoDatabase db = MongoDBConnectionFactory.getDB();
            BasicDBObject updateFields = new BasicDBObject();
            updateFields.append("RequestTime", reRequestTime);
            updateFields.append("ModifiedAt", VinPlayUtils.getCurrentDateTime());
            updateFields.append("UserApprove", userApprove);
            db.getCollection(DEPOSIT_COLLECTION).updateOne((Bson)new Document("CartId", orderId), (Bson)new Document("$set", updateFields));
            return true;
        }
        catch (Exception e) {
            return false;
        }
    }

    @Override
    public Boolean UpdateAmount(String orderId, long amount, long amountFee, String userApprove) {
        try {
            MongoDatabase db = MongoDBConnectionFactory.getDB();
            BasicDBObject updateFields = new BasicDBObject();
            updateFields.append("Amount", amount);
            updateFields.append("AmountFee", amountFee);
            updateFields.append("ModifiedAt", VinPlayUtils.getCurrentDateTime());
            updateFields.append("UserApprove", userApprove);
            db.getCollection(DEPOSIT_COLLECTION).updateOne((Bson)new Document("CartId", orderId), (Bson)new Document("$set", updateFields));
            return true;
        }
        catch (Exception e) {
            return false;
        }
    }

    @Override
    public DepositPaygateModel GetById(String Id) {
        try {
            MongoDatabase db = MongoDBConnectionFactory.getDB();
            Document conditions = new Document();
            conditions.put("Id", Id);
            Document document = (Document)db.getCollection(DEPOSIT_COLLECTION).find((Bson)conditions).first();
            if (document == null) {
                return null;
            }
            DepositPaygateModel model = new DepositPaygateModel(document.getString("Id"), document.getString("CreatedAt"), document.getString("ModifiedAt"), document.getBoolean("IsDeleted"), document.getString("CartId"), document.getString("ReferenceId"), document.getString("UserId"), document.getString("Username"), document.getString("Nickname"), document.getString("RequestTime"), document.getString("BankCode"), document.getString("BankName"), document.getString("PaymentType"), document.getString("MerchantCode"), document.getString("ProviderName"), document.getLong("Amount"), document.getLong("AmountFee"), document.getInteger("Status"), document.getString("BankAccountNumber"), document.getString("BankAccountName"), document.getString("Description"), document.getString("UserApprove"));
            return model;
        }
        catch (Exception e) {
            logger.error(e);
            return null;
        }
    }

    @Override
    public DepositPaygateModel GetByReferenceId(String cartId) {
        try {
            MongoDatabase db = MongoDBConnectionFactory.getDBSlave();
            Document conditions = new Document();
            conditions.put("ReferenceId", cartId);
            Document document = (Document)db.getCollection(DEPOSIT_COLLECTION).find((Bson)conditions).first();
            if (document == null) {
                return null;
            }
            DepositPaygateModel model = new DepositPaygateModel(document.getString("Id"), document.getString("CreatedAt"), document.getString("ModifiedAt"), document.getBoolean("IsDeleted"), document.getString("CartId"), document.getString("ReferenceId"), document.getString("UserId"), document.getString("Username"), document.getString("Nickname"), document.getString("RequestTime"), document.getString("BankCode"), document.getString("BankName"), document.getString("PaymentType"), document.getString("MerchantCode"), document.getString("ProviderName"), document.getLong("Amount"), document.getLong("AmountFee"), document.getInteger("Status"), document.getString("BankAccountNumber"), document.getString("BankAccountName"), document.getString("Description"), document.getString("UserApprove"));
            return model;
        }
        catch (Exception e) {
            logger.error(e);
            return null;
        }
    }

    @Override
    public DepositPaygateModel GetByOrderId(String orderId) {
        try {
            MongoDatabase db = MongoDBConnectionFactory.getDBSlave();
            Document conditions = new Document();
            conditions.put("CartId", orderId);
            Document document = (Document)db.getCollection(DEPOSIT_COLLECTION).find((Bson)conditions).first();
            if (document == null) {
                return null;
            }
            DepositPaygateModel model = new DepositPaygateModel(document.getString("Id"), document.getString("CreatedAt"), document.getString("ModifiedAt"), document.getBoolean("IsDeleted"), document.getString("CartId"), document.getString("ReferenceId"), document.getString("UserId"), document.getString("Username"), document.getString("Nickname"), document.getString("RequestTime"), document.getString("BankCode"), document.getString("BankName"), document.getString("PaymentType"), document.getString("MerchantCode"), document.getString("ProviderName"), document.getLong("Amount"), document.getLong("AmountFee"), document.getInteger("Status"), document.getString("BankAccountNumber"), document.getString("BankAccountName"), document.getString("Description"), document.getString("UserApprove"));
            return model;
        }
        catch (Exception e) {
            logger.error(e);
            return null;
        }
    }

    @Override
    public DepositPaygateReponse Find(DepositPaygateModel depositPayWellModel, int page, int maxItem, String fromTime, String endTime) {
        try {
            final ArrayList<DepositPaygateModel> records = new ArrayList<DepositPaygateModel>();
            final ArrayList<Long> num = new ArrayList<Long>();
            num.add(0, 0L);
            num.add(1, 0L);
            num.add(2, 0L);
            MongoDatabase db = MongoDBConnectionFactory.getDBSlave();
            MongoCollection col = db.getCollection(DEPOSIT_COLLECTION);
            page = page - 1 < 0 ? 0 : page - 1;
            int numStart = page * maxItem;
            int numEnd = maxItem;
            BasicDBObject objsort = new BasicDBObject();
            objsort.put("_id", -1);
            HashMap<String, Object> conditions = new HashMap<String, Object>();
            if (!depositPayWellModel.Nickname.isEmpty()) {
                String pattern = ".*" + depositPayWellModel.Nickname + ".*";
                conditions.put("Nickname", new BasicDBObject().append("$regex", pattern).append("$options", "i"));
            }
            if (!depositPayWellModel.Id.isEmpty()) {
                conditions.put("Id", depositPayWellModel.Id);
            }
            if (!depositPayWellModel.ProviderName.isEmpty()) {
                conditions.put("ProviderName", depositPayWellModel.ProviderName);
            }
            if (depositPayWellModel.Status > 0) {
                conditions.put("Status", depositPayWellModel.Status);
            }
            if (!fromTime.isEmpty() && !endTime.isEmpty()) {
                BasicDBObject obj = new BasicDBObject();
                obj.put("$gte", fromTime);
                obj.put("$lte", endTime);
                conditions.put("CreatedAt", obj);
            }
            FindIterable iterable = col.find((Bson)new Document(conditions)).sort((Bson)objsort).skip(numStart).limit(maxItem);
            iterable.forEach((Block)new Block<Document>(){

                public void apply(Document document) {
                    DepositPaygateModel model = new DepositPaygateModel(document.getString("Id"), document.getString("CreatedAt"), document.getString("ModifiedAt"), document.getBoolean("IsDeleted"), document.getString("CartId"), document.getString("ReferenceId"), document.getString("UserId"), document.getString("Username"), document.getString("Nickname"), document.getString("RequestTime"), document.getString("BankCode"), document.getString("BankName"), document.getString("PaymentType"), document.getString("MerchantCode"), document.getString("ProviderName"), document.getLong("Amount"), document.getLong("AmountFee"), document.getInteger("Status"), document.getString("BankAccountNumber"), document.getString("BankAccountName"), document.getString("Description"), document.getString("UserApprove"));
                    records.add(model);
                }
            });
            FindIterable iterable2 = col.find((Bson)new Document(conditions));
            iterable2.forEach((Block)new Block<Document>(){

                public void apply(Document document) {
                    long amount = document.getLong("Amount");
                    int code = document.getInteger("Status");
                    long count = (Long)num.get(0) + 1L;
                    num.set(0, count);
                    if (code == 100) {
                        long numSuccess = (Long)num.get(1) + 1L;
                        num.set(1, numSuccess);
                        long moneySuccess = (Long)num.get(2) + amount;
                        num.set(2, moneySuccess);
                    }
                }
            });
            DepositPaygateReponse res = new DepositPaygateReponse((Long)num.get(0), (Long)num.get(2), (Long)num.get(1), records);
            return res;
        }
        catch (Exception e) {
            return null;
        }
    }

    @Override
    public DepositPaygateReponse Find(String nickname, int status, int page, int maxItem, String fromTime, String endTime, String providerName) {
        try {
            final ArrayList<DepositPaygateModel> records = new ArrayList<DepositPaygateModel>();
            final ArrayList<Long> num = new ArrayList<Long>();
            num.add(0, 0L);
            num.add(1, 0L);
            num.add(2, 0L);
            MongoDatabase db = MongoDBConnectionFactory.getDBSlave();
            MongoCollection col = db.getCollection(DEPOSIT_COLLECTION);
            page = page - 1 < 0 ? 0 : page - 1;
            int numStart = page * maxItem;
            int numEnd = maxItem;
            BasicDBObject objsort = new BasicDBObject();
            objsort.put("_id", -1);
            HashMap<String, Object> conditions = new HashMap<String, Object>();
            if (!nickname.isEmpty()) {
                String pattern = ".*" + nickname + ".*";
                conditions.put("Nickname", new BasicDBObject().append("$regex", pattern).append("$options", "i"));
            }
            if (!providerName.isEmpty()) {
                conditions.put("ProviderName", providerName);
            }
            if (status > -1) {
                conditions.put("Status", status);
            }
            if (!fromTime.isEmpty() && !endTime.isEmpty()) {
                BasicDBObject obj = new BasicDBObject();
                obj.put("$gte", fromTime);
                obj.put("$lte", endTime);
                conditions.put("CreatedAt", obj);
            }
            FindIterable iterable = col.find((Bson)new Document(conditions)).sort((Bson)objsort).skip(numStart).limit(maxItem);
            iterable.forEach((Block)new Block<Document>(){

                public void apply(Document document) {
                    DepositPaygateModel model = new DepositPaygateModel(document.getString("Id"), document.getString("CreatedAt"), document.getString("ModifiedAt"), document.getBoolean("IsDeleted"), document.getString("CartId"), document.getString("ReferenceId"), document.getString("UserId"), document.getString("Username"), document.getString("Nickname"), document.getString("RequestTime"), document.getString("BankCode"), document.getString("BankName"), document.getString("PaymentType"), document.getString("MerchantCode"), document.getString("ProviderName"), document.getLong("Amount"), document.getLong("AmountFee"), document.getInteger("Status"), document.getString("BankAccountNumber"), document.getString("BankAccountName"), document.getString("Description"), document.getString("UserApprove"));
                    records.add(model);
                }
            });
            FindIterable iterable2 = col.find((Bson)new Document(conditions));
            iterable2.forEach((Block)new Block<Document>(){

                public void apply(Document document) {
                    long amount = document.getLong("Amount");
                    int code = document.getInteger("Status");
                    long count = (Long)num.get(0) + 1L;
                    num.set(0, count);
                    if (code == 100) {
                        long numSuccess = (Long)num.get(1) + 1L;
                        num.set(1, numSuccess);
                        long moneySuccess = (Long)num.get(2) + amount;
                        num.set(2, moneySuccess);
                    }
                }
            });
            DepositPaygateReponse res = new DepositPaygateReponse((Long)num.get(0), (Long)num.get(2), (Long)num.get(1), records);
            return res;
        }
        catch (Exception e) {
            return null;
        }
    }

    @Override
    public Map<String, Object> FindTransaction(String nickname, int status, int page, int maxItem, String fromTime, String endTime, String providerName) {
        HashMap<String, Object> data = new HashMap<String, Object>();
        try {
            BasicDBObject conditions = new BasicDBObject();
            if (!StringUtils.isBlank((String)fromTime) && !StringUtils.isBlank((String)endTime)) {
                BasicDBObject obj = new BasicDBObject();
                obj.put("$gte", fromTime);
                obj.put("$lte", endTime);
                conditions.put("CreatedAt", obj);
            }
            if (!StringUtils.isBlank((String)nickname)) {
                String pattern = ".*" + nickname + ".*";
                conditions.put("Nickname", new BasicDBObject().append("$regex", pattern).append("$options", "i"));
            }
            if (!StringUtils.isBlank((String)providerName)) {
                conditions.put("ProviderName", providerName);
            }
            if (status > -1) {
                conditions.put("Status", status);
            } else {
                BasicDBObject st = new BasicDBObject();
                List<Integer> statusDefault = Arrays.asList(0, 1, 2, 3, 4);
                st.put("$in", statusDefault);
                conditions.append("Status", st);
            }
            Document match = new Document("$match", conditions);
            Document project = new Document();
            project = new Document("$project", new Document("_id", 0).append("CreatedAt", 1).append("Amount", 1).append("AmountFee", 1).append("OrderId", 1).append("BankCode", 1).append("BankAccountNumber", 1).append("BankAccountName", 1).append("ProviderName", 1).append("Status", 1));
            Document sort = new Document();
            sort = new Document("$sort", new Document("CreatedAt", -1));
            MongoDatabase db = MongoDBConnectionFactory.getDBSlave();
            MongoCollection collection = db.getCollection(DEPOSIT_COLLECTION);
            page = page - 1 < 0 ? 0 : page - 1;
            int numStart = page * maxItem;
            int numEnd = maxItem;
            Document skip = new Document();
            skip = new Document("$skip", numStart);
            Document limit = new Document();
            limit = new Document("$limit", numEnd);
            ArrayList<Object> result = new ArrayList<Object>();
            AggregateIterable aggregate = collection.aggregate(Arrays.asList(match, sort, skip, limit, project)).allowDiskUse(Boolean.valueOf(true)).allowDiskUse(Boolean.valueOf(true));
            for (Object _tmpDoc : aggregate) {
                Document document = (Document) _tmpDoc;
                try {
                    String bankCode = document.get("BankCode").toString();
                    Bank bank = GameCommon.LIST_BANK_NAME.stream().filter(x -> bankCode.equals(x.getCode())).findAny().orElse(null);
                    if (bank != null) {
                        document.remove("BankCode");
                        document.append("BankCode", (bank == null ? bankCode : bank.getBank_name()));
                    } else {
                        document.remove("BankCode");
                        switch (bankCode) {
                            case "VTT": {
                                document.append("BankCode", "Viettel");
                                break;
                            }
                            case "VNP": {
                                document.append("BankCode", "Vinaphone");
                                break;
                            }
                            case "VMS": {
                                document.append("BankCode", "Mobiphone");
                                break;
                            }
                            case "VNM": {
                                document.append("BankCode", "VietNam Mobile");
                                break;
                            }
                            case "VCOIN": {
                                document.append("BankCode", "VCOIN");
                                break;
                            }
                            case "ZING": {
                                document.append("BankCode", "ZING");
                                break;
                            }
                            case "GATE": {
                                document.append("BankCode", "FPT GATE");
                                break;
                            }
                            default: {
                                document.append("BankCode", bankCode);
                            }
                        }
                    }
                    result.add(document);
                }
                catch (Exception bankCode) {}
            }
            logger.error(("FindTransaction 1  " + result.size()));
            result.addAll(this.resultDataMomo(nickname, page, maxItem));
            result.addAll(this.resultDataThe(nickname, page, maxItem));
            Collections.sort(result, (o1, o2) -> {
                String bank1 = ((Document)o1).get("CreatedAt").toString();
                String bank2 = ((Document)o2).get("CreatedAt").toString();
                return bank2.compareTo(bank1);
            });
            logger.error(("FindTransaction 2  " + result.size()));
            data.put("data", result);
            Document count = new Document();
            count = new Document("$count", "OrderId");
            int totalRecord = 0;
            AggregateIterable aggregateCount = collection.aggregate(Arrays.asList(match, count)).allowDiskUse(Boolean.valueOf(true));
            for (Object _tmpDoc : aggregateCount) {
                Document document = (Document) _tmpDoc;
                try {
                    totalRecord = document.getInteger("OrderId", 0);
                }
                catch (Exception e) {
                    totalRecord = 0;
                }
            }
            data.put("totalRecord", 100);
        }
        catch (Exception e) {
            logger.error(("Search FindTransaction error: " + e.getMessage()));
            data = new HashMap();
            data.put("data", new ArrayList());
            data.put("totalRecord", 0);
        }
        return data;
    }

    public List<Object> resultDataMomo(String nickname, int page, int maxItem) {
        ArrayList<Object> result = new ArrayList<Object>();
        BasicDBObject conditions = new BasicDBObject();
        if (!StringUtils.isBlank((String)nickname)) {
            String pattern = ".*" + nickname + ".*";
            conditions.put("Nickname", new BasicDBObject().append("$regex", pattern).append("$options", "i"));
        }
        conditions.put("Status", 100);
        Document match = new Document("$match", conditions);
        Document project = new Document();
        project = new Document("$project", new Document("_id", 0).append("CreatedAt", 1).append("Amount", 1).append("AmountFee", 1).append("OrderId", 1).append("BankCode", 1).append("BankAccountNumber", 1).append("BankAccountName", 1).append("ProviderName", 1).append("Status", 1));
        Document sort = new Document();
        sort = new Document("$sort", new Document("CreatedAt", -1));
        MongoDatabase db = MongoDBConnectionFactory.getDBSlave();
        MongoCollection collection = db.getCollection("deposit_momo_manual");
        Document skip = new Document();
        skip = new Document("$skip", (page * maxItem));
        Document limit = new Document();
        limit = new Document("$limit", maxItem);
        AggregateIterable aggregate = collection.aggregate(Arrays.asList(match, sort, skip, limit, project)).allowDiskUse(Boolean.valueOf(true)).allowDiskUse(Boolean.valueOf(true));
        for (Object _tmpDoc : aggregate) {
                Document document = (Document) _tmpDoc;
            document.remove("BankCode");
            document.append("BankCode", "MoMo");
            result.add(document);
            logger.error(("FindTransaction mm" + document.get("Amount").toString()));
        }
        return result;
    }

    public List<Object> resultDataThe(String nickname, int page, int maxItem) {
        ArrayList<Object> result = new ArrayList<Object>();
        BasicDBObject conditions = new BasicDBObject();
        if (!StringUtils.isBlank((String)nickname)) {
            String pattern = ".*" + nickname + ".*";
            conditions.put("nick_name", new BasicDBObject().append("$regex", pattern).append("$options", "i"));
        }
        Document match = new Document("$match", conditions);
        Document project = new Document();
        project = new Document("$project", new Document("_id", 0).append("time_log", 1).append("amount", 1).append("AmountFee", 1).append("OrderId", 1).append("BankCode", 1).append("BankAccountNumber", 1).append("BankAccountName", 1).append("ProviderName", 1).append("code", 1));
        Document sort = new Document();
        sort = new Document("$sort", new Document("time_log", -1));
        MongoDatabase db = MongoDBConnectionFactory.getDBSlave();
        MongoCollection collection = db.getCollection("dvt_recharge_by_gachthe");
        Document skip = new Document();
        skip = new Document("$skip", (page * maxItem));
        Document limit = new Document();
        limit = new Document("$limit", maxItem);
        logger.error(("FindTransaction the  " + limit + " " + maxItem));
        AggregateIterable aggregate = collection.aggregate(Arrays.asList(match, sort, skip, limit, project)).allowDiskUse(Boolean.valueOf(true)).allowDiskUse(Boolean.valueOf(true));
        for (Object _tmpDoc : aggregate) {
                Document document = (Document) _tmpDoc;
            logger.error(("FindTransaction the  " + document));
            document.remove("BankCode");
            document.append("BankCode", "NapThe");
            document.append("CreatedAt", document.get("time_log").toString());
            logger.error(("FindTransaction the 1 " + document));
            document.remove("time_log");
            document.append("Status", document.getInteger("code"));
            logger.error(("FindTransaction the 2 " + document));
            document.remove("code");
            document.append("Amount", document.getLong("amount"));
            logger.error(("FindTransaction the 3 " + document));
            document.remove("amount");
            logger.error(("FindTransaction the 4 " + document));
            result.add(document);
            logger.error(("FindTransaction the  " + document.get("Amount").toString()));
        }
        return result;
    }

    @Override
    public Map<String, Object> FindTransaction(String nickname, String agentCode, int status, int page, int maxItem, String fromTime, String endTime, String providerName) {
        HashMap<String, Object> data = new HashMap<String, Object>();
        try {
            BasicDBObject conditions = new BasicDBObject();
            if (!StringUtils.isBlank((String)fromTime) && !StringUtils.isBlank((String)endTime)) {
                BasicDBObject obj = new BasicDBObject();
                obj.put("$gte", (fromTime + " 00:00:00"));
                obj.put("$lte", (endTime + " 23:59:59"));
                conditions.put("CreatedAt", obj);
            }
            if (!StringUtils.isBlank((String)nickname)) {
                String pattern = ".*" + nickname + ".*";
                conditions.put("Nickname", new BasicDBObject().append("$regex", pattern).append("$options", "i"));
            }
            if (!StringUtils.isBlank((String)agentCode)) {
                conditions.put("MerchantCode", agentCode);
            }
            if (!StringUtils.isBlank((String)providerName)) {
                conditions.put("ProviderName", providerName);
            }
            if (status > -1) {
                conditions.put("Status", status);
            } else {
                BasicDBObject st = new BasicDBObject();
                List<Integer> statusDefault = Arrays.asList(0, 1, 2, 3, 4);
                st.put("$in", statusDefault);
                conditions.append("Status", st);
            }
            Document match = new Document("$match", conditions);
            Document project = new Document();
            project = new Document("$project", new Document("_id", 0).append("CreatedAt", 1).append("Amount", 1).append("Id", 1).append("BankCode", 1).append("BankAccountNumber", 1).append("BankAccountName", 1).append("ProviderName", 1).append("Status", 1).append("MerchantCode", 1).append("CartId", 1).append("Nickname", 1).append("Username", 1).append("Content", 1));
            Document sort = new Document();
            sort = new Document("$sort", new Document("CreatedAt", -1));
            MongoDatabase db = MongoDBConnectionFactory.getDBSlave();
            MongoCollection collection = db.getCollection(DEPOSIT_COLLECTION);
            page = page - 1 < 0 ? 0 : page - 1;
            int numStart = page * maxItem;
            int numEnd = maxItem;
            Document skip = new Document();
            skip = new Document("$skip", numStart);
            Document limit = new Document();
            limit = new Document("$limit", numEnd);
            ArrayList<Document> result = new ArrayList<Document>();
            AggregateIterable aggregate = collection.aggregate(Arrays.asList(match, sort, skip, limit, project)).allowDiskUse(Boolean.valueOf(true)).allowDiskUse(Boolean.valueOf(true));
            for (Object _tmpDoc : aggregate) {
                Document document = (Document) _tmpDoc;
                try {
                    String bankCode = document.get("BankCode").toString();
                    Bank bank = GameCommon.LIST_BANK_NAME.stream().filter(x -> bankCode.equals(x.getCode())).findAny().orElse(null);
                    if (bank != null) {
                        document.remove("BankCode");
                        document.append("BankCode", (bank == null ? bankCode : bank.getBank_name()));
                    }
                    result.add(document);
                }
                catch (Exception bankCode) {}
            }
            data.put("data", result);
            Document count = new Document();
            count = new Document("$count", "Id");
            int totalRecord = 0;
            AggregateIterable aggregateCount = collection.aggregate(Arrays.asList(match, count)).allowDiskUse(Boolean.valueOf(true));
            for (Object _tmpDoc : aggregateCount) {
                Document document = (Document) _tmpDoc;
                try {
                    totalRecord = document.getInteger("Id", 0);
                }
                catch (Exception e) {
                    totalRecord = 0;
                }
            }
            data.put("totalRecord", totalRecord);
        }
        catch (Exception e) {
            logger.error(("Search FindTransaction error: " + e.getMessage()));
            data = new HashMap();
            data.put("data", new ArrayList());
            data.put("totalRecord", 0);
        }
        return data;
    }

    @Override
    public Map<String, Object> FindTransactionUserToAgent(String agentNickname, String userNickName, int status, int page, int maxItem, String fromTime, String endTime, String providerName) {
        HashMap<String, Object> data = new HashMap<String, Object>();
        try {
            BasicDBObject conditions = new BasicDBObject();
            if (!StringUtils.isBlank((String)fromTime) && !StringUtils.isBlank((String)endTime)) {
                BasicDBObject obj = new BasicDBObject();
                obj.put("$gte", new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss Z").parse(fromTime.replace(' ', 'T') + " +0700"));
                obj.put("$lte", new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss Z").parse(endTime.replace(' ', 'T') + " +0700"));
                conditions.put("create_time", obj);
            }
            if (!StringUtils.isBlank((String)agentNickname)) {
                conditions.put("agent", agentNickname);
            }
            if (!StringUtils.isBlank((String)userNickName)) {
                conditions.put("nick_name", userNickName);
            }
            Document match = new Document("$match", conditions);
            Document project = new Document("$project", new Document("_id", 1).append("id", 1).append("user_id", 1).append("nick_name", 1).append("service_name", 1).append("current_money", 1).append("money_exchange", 1).append("description", 1).append("content", 1).append("trans_time", 1).append("action_name", 1).append("fee", 1).append("nick_name", 1).append("create_time", 1).append("agent", 1).append("status", 1));
            Document sort = new Document("$sort", new Document("create_time", -1));
            MongoDatabase db = MongoDBConnectionFactory.getDBSlave();
            MongoCollection collection = db.getCollection("money_user_pay_to_agent");
            page = page - 1 < 0 ? 0 : page - 1;
            int numStart = page * maxItem;
            int numEnd = maxItem;
            Document skip = new Document();
            skip = new Document("$skip", numStart);
            Document limit = new Document();
            limit = new Document("$limit", numEnd);
            ArrayList<Document> result = new ArrayList<Document>();
            AggregateIterable aggregate = collection.aggregate(Arrays.asList(match, sort, skip, limit, project)).allowDiskUse(Boolean.valueOf(true)).allowDiskUse(Boolean.valueOf(true));
            for (Object _tmpDoc : aggregate) {
                Document document = (Document) _tmpDoc;
                try {
                    document.put("_id", document.getObjectId("_id").toString());
                    result.add(document);
                }
                catch (Exception exception) {}
            }
            data.put("data", result);
            Document count = new Document();
            count = new Document("$count", "Id");
            int totalRecord = 0;
            AggregateIterable aggregateCount = collection.aggregate(Arrays.asList(match, count)).allowDiskUse(Boolean.valueOf(true));
            for (Object _tmpDoc : aggregateCount) {
                Document document = (Document) _tmpDoc;
                try {
                    totalRecord = document.getInteger("Id", 0);
                }
                catch (Exception e) {
                    totalRecord = 0;
                }
            }
            data.put("totalRecord", totalRecord);
        }
        catch (Exception e) {
            logger.error(("Search FindTransaction error: " + e.getMessage()));
            data = new HashMap();
            data.put("data", new ArrayList());
            data.put("totalRecord", 0);
        }
        return data;
    }

    public ArrayList<Object> find(HashMap<String, Object> conditions, int page, int maxItem) throws Exception {
        MongoDatabase db = MongoDBConnectionFactory.getDBSlave();
        page = Math.max(page - 1, 0);
        BasicDBObject objsort = new BasicDBObject();
        objsort.put("_id", -1);
        return (ArrayList)db.getCollection(DEPOSIT_COLLECTION).find((Bson)new Document(conditions)).projection(Projections.exclude((String[])new String[]{"_id"})).sort((Bson)objsort).skip(page * maxItem).limit(maxItem).map(x -> JSON.parse((String)x.toJson())).into(new ArrayList());
    }

    @Override
    public long count(HashMap<String, Object> conditions) {
        MongoDatabase db = MongoDBConnectionFactory.getDBSlave();
        return db.getCollection(DEPOSIT_COLLECTION).count((Bson)new Document(conditions));
    }

    @Override
    public Long[] statistic(HashMap<String, Object> conditions) {
        final Long[] num = new Long[]{0L, 0L, 0L, 0L};
        final HashSet set = new HashSet();
        MongoDatabase db = MongoDBConnectionFactory.getDBSlave();
        db.getCollection(DEPOSIT_COLLECTION).find((Bson)new Document(conditions)).forEach((Block)new Block<Document>(){

            public void apply(Document document) {
                Long[] longArray;
                long amount = document.getLong("Amount");
                int code = document.getInteger("Status");
                if (code == 4) {
                    longArray = num;
                    Long l = longArray[0];
                    Long l2 = longArray[0] = Long.valueOf(longArray[0] + 1L);
                    longArray = num;
                    Long.valueOf(longArray[1] + amount);
                }
                longArray = num;
                Long.valueOf(longArray[2] + amount);
                set.add(document.getString("Username"));
            }
        });
        num[3] = (long) set.size();
        return num;
    }

    @Override
    public DepositPaygateReponse Find(String nickname, int status, int page, int maxItem, String fromTime, String endTime, String providerName, String orderId, String transactionId, String bankName, Double fromAmount, Double toAmount) {
        try {
            BasicDBObject obj;
            final ArrayList<DepositPaygateModel> records = new ArrayList<DepositPaygateModel>();
            final ArrayList<Long> num = new ArrayList<Long>();
            num.add(0, 0L);
            num.add(1, 0L);
            num.add(2, 0L);
            MongoDatabase db = MongoDBConnectionFactory.getDBSlave();
            MongoCollection col = db.getCollection(DEPOSIT_COLLECTION);
            page = page - 1 < 0 ? 0 : page - 1;
            int numStart = page * maxItem;
            int numEnd = maxItem;
            BasicDBObject objsort = new BasicDBObject();
            objsort.put("_id", -1);
            HashMap<String, Object> conditions = new HashMap<String, Object>();
            if (nickname != null && !nickname.isEmpty()) {
                String pattern = ".*" + nickname + ".*";
                conditions.put("Nickname", new BasicDBObject().append("$regex", pattern).append("$options", "i"));
            }
            if (providerName != null && !providerName.isEmpty()) {
                conditions.put("ProviderName", providerName);
            }
            if (orderId != null && !orderId.isEmpty()) {
                conditions.put("CartId", orderId);
            }
            if (transactionId != null && !transactionId.isEmpty()) {
                conditions.put("ReferenceId", transactionId);
            }
            if (status >= 0) {
                conditions.put("Status", status);
            }
            if (bankName != null && !"".equals(bankName)) {
                conditions.put("BankCode", bankName);
            }
            if (fromAmount != null) {
                obj = new BasicDBObject();
                obj.put("$gte", fromAmount.longValue());
                conditions.put("Amount", obj);
            }
            if (toAmount != null) {
                obj = new BasicDBObject();
                obj.put("$lte", toAmount.longValue());
                conditions.put("Amount", obj);
            }
            if (fromTime != null && !fromTime.isEmpty() && endTime != null && !endTime.isEmpty()) {
                obj = new BasicDBObject();
                obj.put("$gte", fromTime);
                obj.put("$lte", endTime);
                conditions.put("CreatedAt", obj);
            }
            FindIterable iterable = col.find((Bson)new Document(conditions)).sort((Bson)objsort).skip(numStart).limit(maxItem);
            iterable.forEach((Block)new Block<Document>(){

                public void apply(Document document) {
                    DepositPaygateModel model = new DepositPaygateModel(document.getString("Id"), document.getString("CreatedAt"), document.getString("ModifiedAt"), document.getBoolean("IsDeleted"), document.getString("CartId"), document.getString("ReferenceId"), document.getString("UserId"), document.getString("Username"), document.getString("Nickname"), document.getString("RequestTime"), document.getString("BankCode"), document.getString("BankName"), document.getString("PaymentType"), document.getString("MerchantCode"), document.getString("ProviderName"), document.getLong("Amount"), document.getLong("AmountFee"), document.getInteger("Status"), document.getString("BankAccountNumber"), document.getString("BankAccountName"), document.getString("Description"), document.getString("UserApprove"));
                    records.add(model);
                }
            });
            FindIterable iterable2 = col.find((Bson)new Document(conditions));
            iterable2.forEach((Block)new Block<Document>(){

                public void apply(Document document) {
                    long amount = document.getLong("Amount");
                    int code = document.getInteger("Status");
                    long count = (Long)num.get(0) + 1L;
                    num.set(0, count);
                    if (code == 100) {
                        long numSuccess = (Long)num.get(1) + 1L;
                        num.set(1, numSuccess);
                        long moneySuccess = (Long)num.get(2) + amount;
                        num.set(2, moneySuccess);
                    }
                }
            });
            DepositPaygateReponse res = new DepositPaygateReponse((Long)num.get(0), (Long)num.get(2), (Long)num.get(1), records);
            return res;
        }
        catch (Exception e) {
            logger.error(e);
            return null;
        }
    }

    @Override
    public boolean UpdateTransactionDetail(String agentNickname, String txid, String content, int status) {
        HashMap data = new HashMap();
        try {
            BasicDBObject conditions = new BasicDBObject();
            if (!StringUtils.isBlank((String)agentNickname)) {
                conditions.put("agent", agentNickname);
            }
            if (!StringUtils.isBlank((String)txid)) {
                conditions.put("_id", new ObjectId(txid));
            }
            conditions.put("status", 0);
            BasicDBObject newDocument = new BasicDBObject();
            newDocument.put("content", content);
            newDocument.put("status", status);
            BasicDBObject updateData = new BasicDBObject();
            updateData.put("$set", newDocument);
            MongoDatabase db = MongoDBConnectionFactory.getDBSlave();
            MongoCollection collection = db.getCollection("money_user_pay_to_agent");
            collection.updateOne((Bson)conditions, (Bson)updateData);
            return true;
        }
        catch (Exception e) {
            logger.error(("UpdateTransactionDetail error: " + e.getMessage()));
            return false;
        }
    }
}

