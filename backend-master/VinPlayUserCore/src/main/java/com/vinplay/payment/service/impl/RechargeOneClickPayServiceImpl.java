/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.fasterxml.jackson.databind.ObjectMapper
 *  com.fasterxml.jackson.databind.ObjectWriter
 *  com.google.gson.Gson
 *  com.google.gson.reflect.TypeToken
 *  com.hazelcast.core.HazelcastInstance
 *  com.hazelcast.core.IMap
 *  org.apache.http.NameValuePair
 *  org.apache.http.message.BasicNameValuePair
 *  org.apache.log4j.Logger
 *  org.json.JSONObject
 */
package com.vinplay.payment.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.IMap;
import com.vinplay.dichvuthe.response.RechargePaywellResponse;
import com.vinplay.payment.dao.impl.RechargePaygateDaoImpl;
import com.vinplay.payment.entities.Bank;
import com.vinplay.payment.entities.BankOneClick;
import com.vinplay.payment.entities.Config;
import com.vinplay.payment.entities.DepositPaygateModel;
import com.vinplay.payment.entities.DepositPaygateReponse;
import com.vinplay.payment.entities.PaymentConfig;
import com.vinplay.payment.service.RechargeOneClickPayService;
import com.vinplay.payment.service.impl.PaymentConfigServiceImpl;
import com.vinplay.payment.utils.PayCommon;
import com.vinplay.payment.utils.PayUtils;
import com.vinplay.usercore.logger.MoneyLogger;
import com.vinplay.usercore.utils.GameCommon;
import com.vinplay.vbee.common.hazelcast.HazelcastClientFactory;
import com.vinplay.vbee.common.messagebus.MessageBusFactory;
import com.vinplay.vbee.common.messages.LogMoneyUserMessage;
import com.vinplay.vbee.common.messages.MoneyMessageInMinigame;
import com.vinplay.vbee.common.models.cache.UserCacheModel;
import com.vinplay.vbee.common.messagebus.MessageBusFactory;
import com.vinplay.vbee.common.rmq.RMQApi;
import com.vinplay.vbee.common.utils.VinPlayUtils;
import java.lang.reflect.Type;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import org.apache.http.NameValuePair;
import org.apache.http.message.BasicNameValuePair;
import org.apache.log4j.Logger;
import org.json.JSONObject;

public class RechargeOneClickPayServiceImpl
implements RechargeOneClickPayService {
    private static final Logger logger = Logger.getLogger(RechargeOneClickPayServiceImpl.class);
    private static final String PAYMENTNAME = "clickpay";
    private static final String USERAPPROVE = "system";
    private PaymentConfig paymentConfig = null;

    public RechargeOneClickPayServiceImpl() {
        this.initConfig();
    }

    private void initConfig() {
        this.paymentConfig = new PaymentConfig();
        this.paymentConfig = this.getPaymentConfig();
    }

    private PaymentConfig getPaymentConfig() {
        PaymentConfigServiceImpl paymentConfigService = new PaymentConfigServiceImpl();
        return paymentConfigService.getConfigByKey(PAYMENTNAME);
    }

    private RechargePaywellResponse createOrder(String userId, String username, String nickname, long amount, String bankCode, String paymentType) {
        try {
            RechargePaywellResponse res = new RechargePaywellResponse(1, 0L, 0, 0L, "");
            if (nickname.isEmpty() || amount <= 0L) {
                return res;
            }
            PaymentConfigServiceImpl paymentConfigService = new PaymentConfigServiceImpl();
            PaymentConfig paymentConfig = paymentConfigService.getConfigByKey(PAYMENTNAME);
            RechargePaygateDaoImpl rechargeDao = new RechargePaygateDaoImpl();
            if (rechargeDao.CheckPending(nickname, PAYMENTNAME)) {
                res.setCode(8);
                res.setData("Vui l\u00f2ng \u0111\u1ee3i y\u00eau c\u1ea7u n\u1ea1p ti\u1ec1n tr\u01b0\u1edbc \u0111\u00f3 ho\u00e0n th\u00e0nh");
                return res;
            }
            if (amount < (long)paymentConfig.getConfig().getMinMoney().intValue()) {
                res.setData("So tien nap nho hon so tien quy dinh");
                return res;
            }
            DepositPaygateModel model = new DepositPaygateModel();
            model.Id = "";
            model.Amount = amount;
            model.BankAccountName = "";
            model.BankAccountNumber = "";
            model.BankCode = bankCode;
            model.CartId = "";
            model.CreatedAt = "";
            model.Description = "";
            model.IsDeleted = false;
            model.PaymentType = paymentType;
            model.MerchantCode = paymentConfig.getConfig().getMerchantCode();
            model.ProviderName = PAYMENTNAME;
            model.ModifiedAt = "";
            model.UserId = userId;
            model.Username = username;
            model.Nickname = nickname;
            model.ReferenceId = "";
            model.RequestTime = VinPlayUtils.getCurrentDateTime();
            model.Status = PayCommon.PAYSTATUS.PENDING.getId();
            model.UserApprove = USERAPPROVE;
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

    private RechargePaywellResponse updateRequestTime(String orderId, String requesTime, String userApprove) {
        RechargePaywellResponse res = new RechargePaywellResponse(1, 0L, 0, 0L, "");
        try {
            if (orderId.isEmpty() || requesTime.isEmpty() || userApprove.isEmpty()) {
                return res;
            }
            RechargePaygateDaoImpl rechargeDao = new RechargePaygateDaoImpl();
            DepositPaygateModel model = rechargeDao.GetById(orderId);
            if (model == null) {
                return res;
            }
            if (!rechargeDao.UpdateRequestTime(orderId, requesTime, USERAPPROVE).booleanValue()) {
                return res;
            }
            res.setCode(0);
            return res;
        }
        catch (Exception e) {
            return res;
        }
    }

    private String getSignal(String[] signArray) {
        if (this.paymentConfig == null) {
            this.paymentConfig = this.getPaymentConfig();
        }
        StringBuilder resu = new StringBuilder();
        for (String s : signArray) {
            resu.append(s).append("");
        }
        resu.append(this.paymentConfig.getConfig().getMerchantKey());
        return PayCommon.getMd5(resu.toString());
    }

    private RechargePaywellResponse requestCreateTransaction(String orderId, long amount, String channel, String customerName, String ip, String bankCode) throws Exception {
        RechargePaywellResponse res = new RechargePaywellResponse(1, 0L, 0, 0L, "");
        ZonedDateTime utc = ZonedDateTime.now(ZoneOffset.UTC);
        long timetick = utc.toEpochSecond();
        if (this.updateRequestTime(orderId, String.valueOf(timetick), "superadmin").getCode() != 0) {
            res.setData("");
            return res;
        }
        if (this.paymentConfig == null) {
            this.paymentConfig = this.getPaymentConfig();
        }
        Config config = this.paymentConfig.getConfig();
        String[] paramArray = new String[]{config.getMerchantCode(), orderId, config.getMerchantCode(), String.valueOf(amount), bankCode};
        String sign = this.getSignal(paramArray);
        if (sign.isEmpty()) {
            res.setData("");
            return res;
        }
        ArrayList<NameValuePair> urlParameters = new ArrayList<NameValuePair>();
        urlParameters.add((NameValuePair)new BasicNameValuePair("merchant_id", config.getMerchantCode()));
        urlParameters.add((NameValuePair)new BasicNameValuePair("merchant_txn", orderId));
        urlParameters.add((NameValuePair)new BasicNameValuePair("merchant_customer", config.getMerchantCode()));
        urlParameters.add((NameValuePair)new BasicNameValuePair("amount", String.valueOf(amount)));
        urlParameters.add((NameValuePair)new BasicNameValuePair("bank_code", bankCode));
        urlParameters.add((NameValuePair)new BasicNameValuePair("url_success", config.getNotifyUrl()));
        urlParameters.add((NameValuePair)new BasicNameValuePair("url_error", config.getNotifyUrl()));
        urlParameters.add((NameValuePair)new BasicNameValuePair("description", "Nap tien online"));
        urlParameters.add((NameValuePair)new BasicNameValuePair("sign", sign));
        String result = PayUtils.requestAPI(config.getRequestAPI(), urlParameters);
        logger.info(("clickpayResponse: " + result.toString()));
        JSONObject jsonObj = new JSONObject(result);
        res.setCode(jsonObj.getInt("code"));
        if (jsonObj.getInt("code") != 0) {
            res.setData(jsonObj.getString("message"));
            RechargePaygateDaoImpl rechargePaygateDao = new RechargePaygateDaoImpl();
            rechargePaygateDao.UpdateStatus(orderId, PayCommon.PAYSTATUS.FAILED.getId(), USERAPPROVE);
            res.setCode(99);
        } else {
            res.setData(jsonObj.getString("data"));
        }
        return res;
    }

    private RechargePaywellResponse requestCheckStatusTransaction(String orderId) throws Exception {
        RechargePaywellResponse res = new RechargePaywellResponse(1, 0L, 0, 0L, "");
        try {
            PaymentConfigServiceImpl paymentConfigService = new PaymentConfigServiceImpl();
            PaymentConfig paymentConfig = paymentConfigService.getConfigByKey(PAYMENTNAME);
            Config config = paymentConfig.getConfig();
            String[] paramArray = new String[]{config.getMerchantCode(), orderId};
            String sign = this.getSignal(paramArray);
            if (sign.isEmpty()) {
                res.setData("");
                return res;
            }
            ArrayList<NameValuePair> urlParameters = new ArrayList<NameValuePair>();
            urlParameters.add((NameValuePair)new BasicNameValuePair("merchant_id", config.getMerchantCode()));
            urlParameters.add((NameValuePair)new BasicNameValuePair("mer_txn", orderId));
            urlParameters.add((NameValuePair)new BasicNameValuePair("sign", sign));
            String result = PayUtils.requestAPI(config.getStatusAPI(), urlParameters);
            logger.debug(("clickpay Response: " + result.toString()));
            JSONObject jsonObj = new JSONObject(result.toString());
            res.setCode(jsonObj.getInt("code"));
            Integer code = jsonObj.getInt("code");
            if (code == 0) {
                res.setCode(code);
                String data = jsonObj.getString("data");
                res.setData(data);
                return res;
            }
            return res;
        }
        catch (Exception e) {
            logger.debug(e);
            return res;
        }
    }

    private RechargePaywellResponse requestBanks() throws Exception {
        RechargePaywellResponse res = new RechargePaywellResponse(1, 0L, 0, 0L, "");
        try {
            PaymentConfigServiceImpl paymentConfigService = new PaymentConfigServiceImpl();
            PaymentConfig paymentConfig = paymentConfigService.getConfigByKey(PAYMENTNAME);
            Config config = paymentConfig.getConfig();
            String[] paramArray = new String[]{config.getMerchantCode()};
            String sign = this.getSignal(paramArray);
            if (sign.isEmpty()) {
                res.setData("");
                return res;
            }
            String url = config.getRequestAPI().replace("request", "listBank");
            url = url + "?merchant_id=" + config.getMerchantCode() + "&sign=" + sign;
            String result = PayUtils.requestGetAPI(url);
            JSONObject jsonObj = new JSONObject(result.toString());
            res.setCode(jsonObj.getInt("code"));
            Integer code = jsonObj.getInt("code");
            if (code == 0) {
                res.setCode(code);
                String data = jsonObj.getString("data");
                res.setData(data);
                return res;
            }
            return res;
        }
        catch (Exception e) {
            logger.debug(e);
            return res;
        }
    }

    private String jsonDesc(String desc) {
        return desc;
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    private RechargePaywellResponse addMoney(DepositPaygateModel depositOneClick) {
        RechargePaywellResponse res = new RechargePaywellResponse(1, 0L, 0, 0L, "fail");
        long money = depositOneClick.Amount;
        HazelcastInstance client = HazelcastClientFactory.getInstance();
        if (client == null) {
            MoneyLogger.log(depositOneClick.Nickname, PAYMENTNAME, money, 0L, "vin", "Nap qua ngan hang", "1031", "Cannot connect hazelcast");
            return res;
        }
        IMap userMap = client.getMap("users");
        if (!userMap.containsKey(depositOneClick.Nickname)) {
            return res;
        }
        try {
            userMap.lock(depositOneClick.Nickname);
            UserCacheModel user = (UserCacheModel)userMap.get(depositOneClick.Nickname);
            long moneyUser = user.getVin();
            long currentMoney = user.getVinTotal();
            long rechargeMoney = user.getRechargeMoney();
            user.setVin(moneyUser += money);
            user.setVinTotal(currentMoney += money);
            user.setRechargeMoney(rechargeMoney += money);
            String desc = "ONLINE".equals(depositOneClick.getPayTypeStr()) ? "N\u1ea1p ti\u1ec1n nhanh OneClickPay" : "N\u1ea1p ti\u1ec1n OneClickPay";
            MoneyMessageInMinigame messageMoney = new MoneyMessageInMinigame(VinPlayUtils.genMessageId(), user.getId(), depositOneClick.Nickname, "RechargeByClickPay", moneyUser, currentMoney, money, "vin", 0L, 0, 0);
            LogMoneyUserMessage messageLog = new LogMoneyUserMessage(user.getId(), depositOneClick.Nickname, "RechargeByClickPay", "Nap qua clickpay", currentMoney, money, "vin", desc, 0L, false, user.isBot());
            messageLog.setReferralCode(user.getReferralCode());
            MessageBusFactory.get("queue_payment").publishOrThrow("queue_payment", messageMoney, 16);
            MessageBusFactory.get("queue_log_money").publish("queue_log_money", messageLog, 601);
            userMap.put(depositOneClick.Nickname, user);
            res.setCode(0);
            res.setData("");
        }
        catch (Exception e2) {
            logger.debug(e2);
            MoneyLogger.log(depositOneClick.Nickname, PAYMENTNAME, money, 0L, "vin", "Nap vin qua clickpay", "1031", "rmq error: " + e2.getMessage());
        }
        finally {
            userMap.unlock(depositOneClick.Nickname);
        }
        return res;
    }

    @Override
    public RechargePaywellResponse createTransaction(String userId, String username, String nickname, long amount, String channel, String customerName, String bankCode, String ip) {
        RechargePaywellResponse res = new RechargePaywellResponse(1, 0L, 0, 0L, "");
        try {
            res = this.createOrder(userId, username, nickname, amount, bankCode, channel);
            if (res.getCode() != 0) {
                return res;
            }
            long id = Long.parseLong(res.getTid());
            res = this.requestCreateTransaction(String.valueOf(id), amount, channel, customerName, ip, bankCode);
            return res;
        }
        catch (Exception e) {
            logger.debug(e);
            return res;
        }
    }

    @Override
    public RechargePaywellResponse notify(String amountStr, String netAmountStr, String transactionId, String orderId, String sign) {
        RechargePaywellResponse res = new RechargePaywellResponse(1, 0L, 0, 0L, "");
        RechargePaygateDaoImpl rechargeDao = new RechargePaygateDaoImpl();
        try {
            Double amount;
            try {
                amount = Double.parseDouble(amountStr);
            }
            catch (NumberFormatException e) {
                logger.error(e);
                return res;
            }
            DepositPaygateModel model = rechargeDao.GetById(orderId);
            if (model == null) {
                return res;
            }
            if (model.Amount != amount.longValue()) {
                res.setData("Amount wrong");
                return res;
            }
            if (model.Status != PayCommon.PAYSTATUS.PENDING.getId() && model.Status != PayCommon.PAYSTATUS.RECEIVED.getId()) {
                res.setData("status wrong");
                return res;
            }
            String[] paramArray = new String[]{"merchant_txn=" + transactionId, "merchant_customer=" + model.BankAccountName, "amount=" + model.Amount, "net_amount=" + netAmountStr, "tnx=" + transactionId};
            String signEncode = this.getSignal(paramArray);
            if (!sign.equals(signEncode)) {
                res.setData("Invalid signature");
                return res;
            }
            if (!rechargeDao.UpdateStatus(orderId, transactionId, PayCommon.PAYSTATUS.SUCCESS.getId(), USERAPPROVE).booleanValue()) {
                res.setData("UpdateStatus SUCCESS fail");
                return res;
            }
            res = this.addMoney(model);
            if (res.getCode() != 0) {
                res.setData("Add money fail");
                return res;
            }
            if (!rechargeDao.UpdateStatus(orderId, transactionId, PayCommon.PAYSTATUS.COMPLETED.getId(), USERAPPROVE).booleanValue()) {
                res.setData("UpdateStatus COMPLETED fail");
                return res;
            }
            res.setCode(0);
            return res;
        }
        catch (Exception e) {
            logger.debug(e);
            return res;
        }
    }

    @Override
    public List<BankOneClick> getListBankSupport() {
        try {
            RechargePaywellResponse resResult = this.requestBanks();
            if (resResult.getCode() == 0) {
                Type listType = new TypeToken<List<BankOneClick>>(){}.getType();
                List banks = (List)new Gson().fromJson(resResult.getData(), listType);
                return banks;
            }
            return null;
        }
        catch (Exception e) {
            logger.debug(e);
            return null;
        }
    }

    @Override
    public RechargePaywellResponse checkStatus(String orderId) {
        RechargePaywellResponse res = new RechargePaywellResponse(1, 0L, 0, 0L, "");
        try {
            if (orderId.isEmpty() && orderId.isEmpty()) {
                res.setData("Invalid parram(s)");
                return res;
            }
            RechargePaywellResponse resResult = this.requestCheckStatusTransaction(orderId);
            if (resResult.getCode() == 0) {
                JSONObject jsonObj = new JSONObject(resResult.getData());
                String transaction_id = jsonObj.getString("transaction_id");
                JSONObject jsonObjLog = new JSONObject(jsonObj.getString("log"));
                Integer status = jsonObjLog.getInt("status");
                res.setCode(status);
                res.setData(transaction_id);
            }
            return res;
        }
        catch (Exception e) {
            logger.debug(e);
            return res;
        }
    }

    @Override
    public RechargePaywellResponse getDataTrans(String orderId) {
        RechargePaywellResponse res = new RechargePaywellResponse(1, 0L, 0, 0L, "");
        try {
            if (orderId.isEmpty()) {
                res.setData("Invalid parram(s)");
                return res;
            }
            return this.requestCheckStatusTransaction(orderId);
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
    public List<BankOneClick> getLstOneClickBank() {
        ArrayList<BankOneClick> resultBank = new ArrayList<BankOneClick>();
        try {
            List<Bank> lstBankLote = GameCommon.LIST_BANK_NAME;
            RechargeOneClickPayServiceImpl onePayService = new RechargeOneClickPayServiceImpl();
            long t1 = System.currentTimeMillis();
            List<BankOneClick> banks = onePayService.getListBankSupport();
            long t2 = System.currentTimeMillis();
            logger.info(("check time clickpay " + (t2 - t1)));
            if (banks != null) {
                for (BankOneClick bankOneClick : banks) {
                    for (Bank bankLote : lstBankLote) {
                        if (!bankLote.getCode().equalsIgnoreCase(bankOneClick.code)) continue;
                        bankOneClick.bank_logo = bankLote.getLogo();
                        resultBank.add(bankOneClick);
                    }
                }
            }
            return resultBank;
        }
        catch (Exception e) {
            logger.debug(e);
            return null;
        }
    }
}

