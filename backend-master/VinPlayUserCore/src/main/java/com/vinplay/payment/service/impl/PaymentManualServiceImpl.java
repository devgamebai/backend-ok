/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.hazelcast.core.HazelcastInstance
 *  com.hazelcast.core.IMap
 *  com.mongodb.BasicDBObject
 *  com.mongodb.client.MongoDatabase
 *  org.apache.log4j.Logger
 *  org.bson.Document
 *  org.bson.conversions.Bson
 */
package com.vinplay.payment.service.impl;

import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.IMap;
import com.mongodb.BasicDBObject;
import com.mongodb.client.MongoDatabase;
import com.vinplay.dichvuthe.response.RechargePaywellResponse;
import com.vinplay.payment.dao.impl.RechargePaygateDaoImpl;
import com.vinplay.payment.dao.impl.WithDrawPaygateDaoImpl;
import com.vinplay.payment.entities.DepositPaygateModel;
import com.vinplay.payment.entities.WithDrawPaygateModel;
import com.vinplay.payment.service.PaymentManualService;
import com.vinplay.payment.utils.PayCommon;
import com.vinplay.payment.utils.PayUtils;
import com.vinplay.usercore.logger.MoneyLogger;
import com.vinplay.utils.TelegramAlert;
import com.vinplay.vbee.common.hazelcast.HazelcastClientFactory;
import com.vinplay.vbee.common.messagebus.MessageBusFactory;
import com.vinplay.vbee.common.messages.LogMoneyUserMessage;
import com.vinplay.vbee.common.messages.MoneyMessageInMinigame;
import com.vinplay.vbee.common.models.cache.UserCacheModel;
import com.vinplay.vbee.common.mongodb.MongoDBConnectionFactory;
import com.vinplay.vbee.common.messagebus.MessageBusFactory;
import com.vinplay.vbee.common.rmq.RMQApi;
import com.vinplay.vbee.common.utils.VinPlayUtils;
import org.apache.log4j.Logger;
import org.bson.Document;
import org.bson.conversions.Bson;

public class PaymentManualServiceImpl
implements PaymentManualService {
    private static final Logger logger = Logger.getLogger((String)"backend");
    private static final String PAYMENTNAME = "manual";

    @Override
    public RechargePaywellResponse withdrawal(String orderId, String approvedName, String ip) {
        return this.withdrawal(orderId, approvedName, ip, PayCommon.PAYSTATUS.COMPLETED);
    }

    @Override
    public RechargePaywellResponse withdrawal(String orderId, String approvedName, String ip, PayCommon.PAYSTATUS paystatus) {
        return this.withdrawal(orderId, approvedName, ip, paystatus, null);
    }

    @Override
    public RechargePaywellResponse withdrawal(String orderId, String approvedName, String ip, PayCommon.PAYSTATUS paystatus, String provider) {
        RechargePaywellResponse res = new RechargePaywellResponse(1, 0L, 0, 0L, "");
        try {
            WithDrawPaygateDaoImpl withdrawDao = new WithDrawPaygateDaoImpl();
            WithDrawPaygateModel model = withdrawDao.GetByOrderId(orderId);
            if (model == null) {
                res.setData("Object does not exist");
                return res;
            }
            if (PayCommon.PAYSTATUS.REQUEST.getId() != model.Status) {
                res.setData("status wrong , status = " + model.Status);
                return res;
            }
            if (model.Amount < 100000L) {
                res.setData("So tien rut nho hon so tien quy dinh");
                return res;
            }
            String transID = "WD" + PayUtils.getCurDate("yyMMddHHmmss") + PayUtils.getids();
            boolean isUpdate = this.updateStatus(orderId, transID, paystatus.getId(), approvedName, ip, provider);
            if (isUpdate) {
                res.setCode(0);
                res.setData("");
            } else {
                res.setCode(1);
                res.setData("update status to COMPLETED was fail");
            }
            return res;
        }
        catch (Exception e) {
            logger.error(e);
            return res;
        }
    }

    @Override
    public Boolean withdrawalSystemNote(String orderId, PayCommon.PAYSTATUS paystatus, String note) {
        try {
            MongoDatabase db = MongoDBConnectionFactory.getDB();
            BasicDBObject updateFields = new BasicDBObject();
            updateFields.append("Status", paystatus.getId());
            updateFields.append("Description", note);
            updateFields.append("ModifiedAt", VinPlayUtils.getCurrentDateTime());
            db.getCollection("withdrawal_paygate").updateOne((Bson)new Document("CartId", orderId), (Bson)new Document("$set", updateFields));
            return true;
        }
        catch (Exception e) {
            return false;
        }
    }

    public Boolean updateStatus(String orderId, String cartId, int status, String userApprove, String ip, String provider) {
        try {
            MongoDatabase db = MongoDBConnectionFactory.getDB();
            BasicDBObject updateFields = new BasicDBObject();
            updateFields.append("ReferenceId", cartId);
            updateFields.append("Status", status);
            if (provider != null) {
                updateFields.append("ProviderName", provider);
                updateFields.append("MerchantCode", provider);
            } else {
                updateFields.append("ProviderName", "manualbank");
                updateFields.append("MerchantCode", "manualbank");
            }
            updateFields.append("ModifiedAt", VinPlayUtils.getCurrentDateTime());
            updateFields.append("UserApprove", userApprove);
            updateFields.append("IPApprove", ip);
            db.getCollection("withdrawal_paygate").updateOne((Bson)new Document("CartId", orderId), (Bson)new Document("$set", updateFields));
            return true;
        }
        catch (Exception e) {
            return false;
        }
    }

    private RechargePaywellResponse createOrder(String nickName, String customerName, Long amount, String bankName, String bankCode, String bankNum, String payType, String desc) {
        try {
            RechargePaywellResponse res = new RechargePaywellResponse(1, 0L, 0, 0L, "");
            if (amount < 20000L) {
                res.setData("S\u1ed1 ti\u1ec1n n\u1ea1p nh\u1ecf h\u01a1n 20.000 VN\u0110");
                return res;
            }
            DepositPaygateModel model = new DepositPaygateModel();
            model.Id = "";
            model.Amount = amount;
            model.BankAccountName = customerName;
            model.BankAccountNumber = bankNum;
            model.BankName = bankName;
            model.BankCode = bankCode;
            model.CartId = "";
            model.CreatedAt = "";
            model.IsDeleted = false;
            model.PaymentType = payType;
            model.MerchantCode = PAYMENTNAME;
            model.ProviderName = PAYMENTNAME;
            model.ModifiedAt = "";
            model.Nickname = nickName;
            model.ReferenceId = "";
            model.RequestTime = VinPlayUtils.getCurrentDateTime();
            model.Status = PayCommon.PAYSTATUS.PENDING.getId();
            model.Description = desc;
            model.UserApprove = nickName;
            RechargePaygateDaoImpl rechargeDao = new RechargePaygateDaoImpl();
            long id = rechargeDao.Add(model);
            if (id == 0L) {
                logger.error("L\u1ed7i t\u1ea1o b\u1ea3n ghi database mongo");
                return res;
            }
            res.setCode(0);
            res.setTid(String.valueOf(id));
            TelegramAlert.SendMessageDepositBank(model);
            return res;
        }
        catch (Exception e) {
            return null;
        }
    }

    @Override
    public RechargePaywellResponse deposit(String nickName, String customerName, Long amount, String bankName, String bankCode, String bankNum, String payType, String desc) {
        try {
            return this.createOrder(nickName, customerName, amount, bankName, bankCode, bankNum, payType, desc);
        }
        catch (Exception e) {
            logger.error(e);
            return new RechargePaywellResponse(1, 0L, 0, 0L, e.getMessage());
        }
    }

    @Override
    public RechargePaywellResponse depositConfirm(String orderId, String approvedName, String ip, int status, String rs) {
        RechargePaygateDaoImpl dao = new RechargePaygateDaoImpl();
        DepositPaygateModel modelOld = dao.GetByOrderId(orderId);
        if (modelOld != null) {
            int statusOld = modelOld.Status;
            if (statusOld != PayCommon.PAYSTATUS.PENDING.getId()) {
                return new RechargePaywellResponse(1, 0L, 0, 0L, "Ch\u1ec9 c\u00f3 th\u1ec3 ph\u00ea duy\u1ec7t \u0111c t\u1eeb tr\u1ea1ng th\u00e1i pending");
            }
            String providerName = modelOld.ProviderName;
            if (!PAYMENTNAME.equals(providerName)) {
                return new RechargePaywellResponse(1, 0L, 0, 0L, "Ch\u1ec9 c\u00f3 th\u1ec3 ph\u00ea duy\u1ec7t \u0111c t\u1eeb h\u00ecnh th\u1ee9c n\u1ea1p th\u1ee7 c\u00f4ng");
            }
        } else {
            return new RechargePaywellResponse(1, 0L, 0, 0L, "Ch\u1ec9 c\u00f3 th\u1ec3 ph\u00ea duy\u1ec7t \u0111c t\u1eeb h\u00ecnh th\u1ee9c n\u1ea1p th\u1ee7 c\u00f4ng");
        }
        modelOld.Status = status;
        modelOld.UserApprove = approvedName;
        modelOld.Description = rs;
        modelOld.ReferenceId = orderId;
        boolean isuc = dao.UpdateStatus(orderId, orderId, status, approvedName, rs);
        if (isuc) {
            if (status == PayCommon.PAYSTATUS.COMPLETED.getId()) {
                return this.addMoney(modelOld);
            }
            return new RechargePaywellResponse(0, 0L, 0, 0L, "");
        }
        return new RechargePaywellResponse(1, 0L, 0, 0L, "");
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    private RechargePaywellResponse addMoney(DepositPaygateModel depositPayWellModel) {
        RechargePaywellResponse res = new RechargePaywellResponse(1, 0L, 0, 0L, "fail");
        HazelcastInstance client = HazelcastClientFactory.getInstance();
        if (client == null) {
            MoneyLogger.log(depositPayWellModel.Nickname, PAYMENTNAME, depositPayWellModel.Amount, 0L, "vin", "Nap vin qua ngan hang", "1031", "Cannot connect hazelcast");
            return res;
        }
        IMap userMap = client.getMap("users");
        if (!userMap.containsKey(depositPayWellModel.Nickname)) {
            return res;
        }
        try {
            userMap.lock(depositPayWellModel.Nickname);
            UserCacheModel user = (UserCacheModel)userMap.get(depositPayWellModel.Nickname);
            long moneyUser = user.getVin();
            long currentMoney = user.getVinTotal();
            long rechargeMoney = user.getRechargeMoney();
            user.setVin(moneyUser += depositPayWellModel.Amount);
            user.setVinTotal(currentMoney += depositPayWellModel.Amount);
            user.setRechargeMoney(rechargeMoney += depositPayWellModel.Amount);
            String desc = "N\u1ea1p Ti\u1ec1n";
            MoneyMessageInMinigame messageMoney = new MoneyMessageInMinigame(VinPlayUtils.genMessageId(), user.getId(), depositPayWellModel.Nickname, "RechargeByPaywell", moneyUser, currentMoney, depositPayWellModel.Amount, "vin", 0L, 0, 0);
            LogMoneyUserMessage messageLog = new LogMoneyUserMessage(user.getId(), depositPayWellModel.Nickname, "RechargeByPaywell", PAYMENTNAME, currentMoney, depositPayWellModel.Amount, "vin", desc, 0L, false, user.isBot());
            messageLog.setReferralCode(user.getReferralCode());
            MessageBusFactory.get("queue_payment").publishOrThrow("queue_payment", messageMoney, 16);
            MessageBusFactory.get("queue_log_money").publish("queue_log_money", messageLog, 601);
            userMap.put(depositPayWellModel.Nickname, user);
            res.setCode(0);
            res.setData("");
        }
        catch (Exception e2) {
            logger.debug(e2);
            MoneyLogger.log(depositPayWellModel.Nickname, PAYMENTNAME, depositPayWellModel.Amount, 0L, "vin", "Nap vin qua manual", "1031", "rmq error: " + e2.getMessage());
        }
        finally {
            userMap.unlock(depositPayWellModel.Nickname);
        }
        return res;
    }
}

