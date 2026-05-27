/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.fasterxml.jackson.databind.ObjectMapper
 *  com.fasterxml.jackson.databind.ObjectWriter
 *  com.google.gson.Gson
 *  com.hazelcast.core.HazelcastInstance
 *  com.hazelcast.core.IMap
 *  org.apache.commons.lang3.StringUtils
 *  org.apache.log4j.Logger
 *  org.json.JSONObject
 */
package com.vinplay.payment.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.google.gson.Gson;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.IMap;
import com.vinplay.dichvuthe.response.RechargePaywellResponse;
import com.vinplay.payment.dao.impl.RechargePaygateDaoImpl;
import com.vinplay.payment.entities.Config;
import com.vinplay.payment.entities.DepositPaygateModel;
import com.vinplay.payment.entities.DepositPaygateReponse;
import com.vinplay.payment.entities.PaymentConfig;
import com.vinplay.payment.service.RechargePayaSecService;
import com.vinplay.payment.service.impl.PaymentConfigServiceImpl;
import com.vinplay.payment.utils.PayCommon;
import com.vinplay.payment.utils.PayUtils;
import com.vinplay.usercore.logger.MoneyLogger;
import com.vinplay.vbee.common.hazelcast.HazelcastClientFactory;
import com.vinplay.vbee.common.messagebus.MessageBusFactory;
import com.vinplay.vbee.common.messages.LogMoneyUserMessage;
import com.vinplay.vbee.common.messages.MoneyMessageInMinigame;
import com.vinplay.vbee.common.models.cache.UserCacheModel;
import com.vinplay.vbee.common.response.BaseResponse;
import com.vinplay.vbee.common.messagebus.MessageBusFactory;
import com.vinplay.vbee.common.rmq.RMQApi;
import com.vinplay.vbee.common.utils.VinPlayUtils;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;
import org.json.JSONObject;

public class RechargePayaSecServiceImpl
implements RechargePayaSecService {
    private static final Logger logger = Logger.getLogger(RechargePayaSecServiceImpl.class);
    private static final String PAYMENTNAME = "payasec";
    private static final String USERAPPROVE = "system";
    private static final List<Integer> RIGHT_STATUS = Arrays.asList(0, 1, 2, 3, 4, 11);

    private PaymentConfig getPaymentConfig() {
        PaymentConfigServiceImpl paymentConfigService = new PaymentConfigServiceImpl();
        return paymentConfigService.getConfigByKey(PAYMENTNAME);
    }

    private RechargePaywellResponse createOrder(String userId, String username, String nickname, long amount, String typeCard, String serial, String pin) {
        try {
            RechargePaywellResponse res = new RechargePaywellResponse(1, 0L, 0, 0L, "");
            if (StringUtils.isBlank((CharSequence)nickname) || amount <= 0L) {
                return res;
            }
            PaymentConfigServiceImpl paymentConfigService = new PaymentConfigServiceImpl();
            PaymentConfig paymentConfig = paymentConfigService.getConfigByKey(PAYMENTNAME);
            if (amount < (long)paymentConfig.getConfig().getMinMoney().intValue()) {
                res.setData("So tien nap nho hon so tien quy dinh");
                return res;
            }
            RechargePaygateDaoImpl rechargeDao = new RechargePaygateDaoImpl();
            if (rechargeDao.CheckPending(nickname, PAYMENTNAME)) {
                res.setCode(8);
                res.setData("Vui l\u00f2ng \u0111\u1ee3i y\u00eau c\u1ea7u n\u1ea1p ti\u1ec1n tr\u01b0\u1edbc \u0111\u00f3 ho\u00e0n th\u00e0nh");
                return res;
            }
            Double ratio = 0.0;
            switch (typeCard) {
                case "VTT": {
                    ratio = 24.0;
                    break;
                }
                case "VNP": 
                case "VMS": 
                case "VNM": {
                    ratio = 26.0;
                    break;
                }
                case "ZING": 
                case "VCOIN": 
                case "GATE": {
                    ratio = 32.0;
                }
            }
            Double fee = (double)amount * (ratio / 100.0);
            Double netAmount = (double)amount - fee;
            DepositPaygateModel model = new DepositPaygateModel();
            model.Id = "";
            model.Amount = netAmount.longValue();
            model.AmountFee = fee.longValue();
            model.BankAccountName = "";
            model.BankAccountNumber = "";
            model.BankCode = typeCard;
            model.CartId = "";
            model.CreatedAt = "";
            model.Description = "SN: " + serial + " | PIN: " + pin;
            model.IsDeleted = false;
            model.PaymentType = "IB_ONLINE";
            model.MerchantCode = paymentConfig.getConfig().getMerchantCode();
            model.ProviderName = PAYMENTNAME;
            model.ModifiedAt = "";
            model.UserId = userId;
            model.Username = username;
            model.Nickname = nickname;
            model.ReferenceId = "";
            model.RequestTime = "";
            model.Status = PayCommon.PAYSTATUS.PENDING.getId();
            model.UserApprove = nickname;
            long id = rechargeDao.Add(model);
            if (id == 0L) {
                return res;
            }
            res.setCode(0);
            res.setTid(String.valueOf(id));
            return res;
        }
        catch (Exception e) {
            return null;
        }
    }

    private RechargePaywellResponse requestCreateTransaction(DepositPaygateModel model) throws Exception {
        RechargePaywellResponse res = new RechargePaywellResponse(1, 0L, 0, 0L, "");
        String[] arrdes = model.Description.split(" | ");
        String serial = arrdes[1].trim();
        String pin = arrdes[4].trim();
        PaymentConfigServiceImpl paymentConfigService = new PaymentConfigServiceImpl();
        PaymentConfig paymentConfig = paymentConfigService.getConfigByKey(PAYMENTNAME);
        Config config = paymentConfig.getConfig();
        String signature_string = config.getMerchantCode() + "|" + model.Id + "|SC|" + model.BankCode + "|" + pin + "|" + serial + "|" + (model.Amount + model.AmountFee);
        String hash = PayCommon.getHMACSHA256(config.getMerchantKey(), signature_string);
        if (hash.isEmpty()) {
            res.setData("");
            return res;
        }
        HashMap<String, Object> params = new HashMap<String, Object>();
        params.put("MID", config.getMerchantCode());
        params.put("refIdPartner", model.Id);
        params.put("currency", config.getCurrencyCode());
        params.put("gateway", "SC");
        params.put("amount", model.Amount + model.AmountFee);
        params.put("returnUrl", config.getNotifyUrl());
        params.put("telco", model.BankCode);
        params.put("pin", pin);
        params.put("serial", serial);
        params.put("token", hash);
        Gson gson = new Gson();
        String body = gson.toJson(params);
        String result = PayUtils.requestAPIs(config.getRequestAPI(), body);
        logger.info(("payasecResponse: " + result.toString()));
        RechargePaygateDaoImpl rechargePaygateDao = new RechargePaygateDaoImpl();
        try {
            JSONObject jsonObj = new JSONObject(result);
            res.setCode(jsonObj.getInt("errCode"));
            res.setData(jsonObj.getString("mess"));
            if (jsonObj.getInt("errCode") != 0) {
                rechargePaygateDao.Delete(model.Id);
            } else {
                JSONObject data = new JSONObject(jsonObj.getString("data"));
                String refId = data.getString("refId");
                if (StringUtils.isBlank((CharSequence)refId)) {
                    rechargePaygateDao.Delete(model.Id);
                    res.setCode(99);
                    res.setData("Can not get refId");
                    return res;
                }
                String refIdPartner = data.getString("refIdPartner");
                if (StringUtils.isBlank((CharSequence)refIdPartner)) {
                    rechargePaygateDao.Delete(model.Id);
                    res.setCode(99);
                    res.setData("Can not get refIdPartner");
                    return res;
                }
                if (!refIdPartner.equals(model.Id)) {
                    rechargePaygateDao.Delete(model.Id);
                    res.setData("refIdPartner not match");
                    res.setCode(1001);
                    return res;
                }
                model.ReferenceId = refId;
                model.Status = PayCommon.PAYSTATUS.RECEIVED.getId();
                rechargePaygateDao.Update(model);
            }
        }
        catch (Exception e) {
            rechargePaygateDao.Delete(model.Id);
            res.setCode(1001);
            res.setData(result);
        }
        return res;
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    private RechargePaywellResponse addMoney(DepositPaygateModel depositPayWellModel) {
        RechargePaywellResponse res = new RechargePaywellResponse(1, 0L, 0, 0L, "fail");
        HazelcastInstance client = HazelcastClientFactory.getInstance();
        if (client == null) {
            MoneyLogger.log(depositPayWellModel.Nickname, PAYMENTNAME, depositPayWellModel.Amount, 0L, "vin", "Nap vin qua payasec", "1031", "Cannot connect hazelcast");
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
            String desc = "N\u1ea1p qua th\u1ebb c\u00e0o (Payasec)";
            MoneyMessageInMinigame messageMoney = new MoneyMessageInMinigame(VinPlayUtils.genMessageId(), user.getId(), depositPayWellModel.Nickname, "RechargeBySC", moneyUser, currentMoney, depositPayWellModel.Amount, "vin", 0L, 0, 0);
            LogMoneyUserMessage messageLog = new LogMoneyUserMessage(user.getId(), depositPayWellModel.Nickname, "RechargeBySC", PAYMENTNAME, currentMoney, depositPayWellModel.Amount, "vin", desc, 0L, false, user.isBot());
            messageLog.setReferralCode(user.getReferralCode());
            MessageBusFactory.get("queue_payment").publishOrThrow("queue_payment", messageMoney, 16);
            MessageBusFactory.get("queue_log_money").publish("queue_log_money", messageLog, 601);
            userMap.put(depositPayWellModel.Nickname, user);
            res.setCode(0);
            res.setData("");
        }
        catch (Exception e2) {
            logger.debug(e2);
            MoneyLogger.log(depositPayWellModel.Nickname, PAYMENTNAME, depositPayWellModel.Amount, 0L, "vin", "Nap vin qua payasec", "1031", "rmq error: " + e2.getMessage());
        }
        finally {
            userMap.unlock(depositPayWellModel.Nickname);
        }
        return res;
    }

    @Override
    public RechargePaywellResponse createTransaction(String userId, String username, String nickname, String fullName, long amount, String typeCard, String serial, String pin) {
        RechargePaywellResponse res = new RechargePaywellResponse(1, 0L, 0, 0L, "");
        try {
            res = this.createOrder(userId, username, nickname, amount, typeCard, serial, pin);
            if (res.getCode() != 0) {
                return res;
            }
            long id = Long.parseLong(res.getTid());
            RechargePaygateDaoImpl rechargeDao = new RechargePaygateDaoImpl();
            DepositPaygateModel model = rechargeDao.GetById(String.valueOf(id));
            res = this.requestCreateTransaction(model);
            return res;
        }
        catch (Exception e) {
            logger.debug(e);
            return res;
        }
    }

    @Override
    public RechargePaywellResponse find(String nickname, int status, int page, int maxItem, String fromTime, String endTime, String providerName) {
        RechargePaywellResponse res = new RechargePaywellResponse(1, 0L, 0, 0L, "");
        try {
            RechargePaygateDaoImpl rechargeDao = new RechargePaygateDaoImpl();
            DepositPaygateReponse depositPayWellReponses = rechargeDao.Find(nickname, status, page, maxItem, fromTime, endTime, providerName);
            ObjectWriter ow = new ObjectMapper().writer().withDefaultPrettyPrinter();
            String json = ow.writeValueAsString(depositPayWellReponses);
            res.setCode(0);
            res.setData(json.toString());
            return res;
        }
        catch (Exception e) {
            logger.debug(e);
            return res;
        }
    }

    @Override
    public DepositPaygateReponse search(String nickname, int status, int page, int maxItem, String fromTime, String endTime, String providerName) {
        DepositPaygateReponse depositPayWellReponses = new DepositPaygateReponse(0L, 0L, 0L, new ArrayList<DepositPaygateModel>());
        try {
            RechargePaygateDaoImpl rechargeDao = new RechargePaygateDaoImpl();
            depositPayWellReponses = rechargeDao.Find(nickname, status, page, maxItem, fromTime, endTime, providerName);
            return depositPayWellReponses;
        }
        catch (Exception e) {
            logger.debug(e);
            return depositPayWellReponses;
        }
    }

    @Override
    public Map<String, Object> FindTransaction(String nickname, int status, int page, int maxItem, String fromTime, String endTime, String providerName) {
        Map<String, Object> data = new HashMap();
        try {
            RechargePaygateDaoImpl dao = new RechargePaygateDaoImpl();
            data = dao.FindTransaction(nickname, status, page, maxItem, fromTime, endTime, providerName);
            return data;
        }
        catch (Exception e) {
            logger.debug(e);
            return new HashMap<String, Object>();
        }
    }

    private BaseResponse<String> validateNotify(String created, String updated, String refId, String refIdPartner, String gateway, String gatewayDetail, long amount, long fee, long netAmount, int status, String token) {
        if (StringUtils.isBlank((CharSequence)created)) {
            return new BaseResponse<String>("5", "created is null or empty");
        }
        if (StringUtils.isBlank((CharSequence)updated)) {
            return new BaseResponse<String>("5", "created is null or empty");
        }
        if (StringUtils.isBlank((CharSequence)refId)) {
            return new BaseResponse<String>("5", "created is null or empty");
        }
        if (StringUtils.isBlank((CharSequence)refIdPartner)) {
            return new BaseResponse<String>("5", "created is null or empty");
        }
        if (StringUtils.isBlank((CharSequence)gateway)) {
            return new BaseResponse<String>("5", "created is null or empty");
        }
        if (StringUtils.isBlank((CharSequence)gatewayDetail)) {
            return new BaseResponse<String>("5", "created is null or empty");
        }
        if (StringUtils.isBlank((CharSequence)token)) {
            return new BaseResponse<String>("5", "created is null or empty");
        }
        if (amount <= 0L) {
            return new BaseResponse<String>("5", "amount < 0");
        }
        if (fee >= amount) {
            return new BaseResponse<String>("5", "amountFee >= amount");
        }
        if (netAmount <= 0L) {
            return new BaseResponse<String>("5", "netAmount < 0");
        }
        if (!RIGHT_STATUS.contains(status)) {
            return new BaseResponse<String>("5", "status wrong ,status=" + status);
        }
        return new BaseResponse<String>(true, "0", "SUCCESS", null);
    }

    @Override
    public RechargePaywellResponse notification(String created, String updated, String refId, String refIdPartner, String gateway, String gatewayDetail, long amount, long fee, long netAmount, int status, String token) {
        RechargePaywellResponse res;
        block15: {
            res = new RechargePaywellResponse(1, 0L, 0, 0L, "");
            if (!PayUtils.validateRequest(refIdPartner)) {
                res.setData("Must be at least 2 seconds for next request");
                return res;
            }
            BaseResponse<String> valid = this.validateNotify(created, updated, refId, refIdPartner, gateway, gatewayDetail, amount, fee, netAmount, status, token);
            if ("0".equals(valid.getErrorCode())) {
                RechargePaygateDaoImpl rechargeDao = new RechargePaygateDaoImpl();
                try {
                    DepositPaygateModel model = rechargeDao.GetById(refIdPartner);
                    if (model == null) {
                        return res;
                    }
                    if (model.Status == PayCommon.PAYSTATUS.COMPLETED.getId()) {
                        res.setCode(0);
                        return res;
                    }
                    if (model.Status != PayCommon.PAYSTATUS.PENDING.getId() && model.Status != PayCommon.PAYSTATUS.RECEIVED.getId() && model.Status != PayCommon.PAYSTATUS.FAILED.getId()) {
                        res.setData("status wrong");
                        return res;
                    }
                    Config config = this.getPaymentConfig().getConfig();
                    String signature_string = created + "|" + updated + "|" + refId + "|" + model.Id + "|" + gateway + "|" + gatewayDetail + "|" + amount + "|" + fee + "|" + netAmount + "|" + status;
                    String hash = PayCommon.getHMACSHA256(config.getMerchantKey(), signature_string);
                    if (hash.isEmpty()) {
                        res.setData("");
                        return res;
                    }
                    if (!token.equals(hash)) {
                        res.setData("Invalid signature");
                        return res;
                    }
                    if (status == 2) {
                        boolean isUpdate = rechargeDao.UpdateStatus(model.CartId, refId, PayCommon.PAYSTATUS.FAILED.getId(), USERAPPROVE);
                        if (!isUpdate) {
                            logger.error("payasec unable update status to fail");
                        }
                        res.setData("status updated to fail");
                        return res;
                    }
                    if (status == 1) {
                        if (model.Amount > netAmount) {
                            model.Description = model.Description + " | (OR)AM:" + model.Amount + " | FE:" + model.AmountFee;
                            model.Amount = netAmount;
                            model.AmountFee = amount - netAmount;
                        }
                        model.ReferenceId = refId;
                        model.Status = PayCommon.PAYSTATUS.COMPLETED.getId();
                        model.UserApprove = USERAPPROVE;
                        if (!rechargeDao.Update(model).booleanValue()) {
                            res.setData("UpdateStatus SUCCESS fail");
                            return res;
                        }
                        res = this.addMoney(model);
                        if (res.getCode() != 0) {
                            res.setData("Add money fail");
                            return res;
                        }
                        res.setCode(0);
                        return res;
                    }
                    break block15;
                }
                catch (Exception e) {
                    logger.debug(e);
                    return res;
                }
            }
            res.setCode(Integer.parseInt(valid.getErrorCode()));
            res.setData(valid.getMessage());
        }
        return res;
    }
}

