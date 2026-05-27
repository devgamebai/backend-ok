/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.fasterxml.jackson.databind.ObjectMapper
 *  com.fasterxml.jackson.databind.ObjectWriter
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
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.IMap;
import com.vinplay.dichvuthe.response.RechargePaywellResponse;
import com.vinplay.payment.dao.impl.RechargePaygateDaoImpl;
import com.vinplay.payment.entities.Config;
import com.vinplay.payment.entities.DepositPaygateModel;
import com.vinplay.payment.entities.DepositPaygateReponse;
import com.vinplay.payment.entities.PaymentConfig;
import com.vinplay.payment.service.RechargePrincePayService;
import com.vinplay.payment.service.impl.PaymentConfigServiceImpl;
import com.vinplay.payment.utils.PayCommon;
import com.vinplay.payment.utils.PayUtils;
import com.vinplay.usercore.logger.MoneyLogger;
import com.vinplay.vbee.common.hazelcast.HazelcastClientFactory;
import com.vinplay.vbee.common.messagebus.MessageBusFactory;
import com.vinplay.vbee.common.messages.LogMoneyUserMessage;
import com.vinplay.vbee.common.messages.MoneyMessageInMinigame;
import com.vinplay.vbee.common.models.cache.UserCacheModel;
import com.vinplay.vbee.common.messagebus.MessageBusFactory;
import com.vinplay.vbee.common.rmq.RMQApi;
import com.vinplay.vbee.common.utils.VinPlayUtils;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import org.apache.http.NameValuePair;
import org.apache.http.message.BasicNameValuePair;
import org.apache.log4j.Logger;
import org.json.JSONObject;

public class RechargePrincePayServiceImpl
implements RechargePrincePayService {
    private static final Logger logger = Logger.getLogger(RechargePrincePayServiceImpl.class);
    private static final String PAYMENTNAME = "princepay";
    private static final String USERAPPROVE = "system";

    private PaymentConfig getPaymentConfig() {
        PaymentConfigServiceImpl paymentConfigService = new PaymentConfigServiceImpl();
        return paymentConfigService.getConfigByKey(PAYMENTNAME);
    }

    private RechargePaywellResponse createOrder(String userId, String username, String nickname, long amount, String bankCode, String paymentType) {
        try {
            RechargePaygateDaoImpl rechargeDao;
            RechargePaywellResponse res = new RechargePaywellResponse(1, 0L, 0, 0L, "");
            if (nickname.isEmpty() || amount <= 0L) {
                return res;
            }
            PaymentConfigServiceImpl paymentConfigService = new PaymentConfigServiceImpl();
            PaymentConfig paymentConfig = paymentConfigService.getConfigByKey(PAYMENTNAME);
            if (paymentType.equals("923")) {
                if (amount < 20000L) {
                    res.setData("S\u1ed1 ti\u1ec1n n\u1ea1p nh\u1ecf h\u01a1n 20.000 VN\u0110");
                    return res;
                }
                if (amount > 20000000L) {
                    res.setData("S\u1ed1 ti\u1ec1n n\u1ea1p l\u1edbn h\u01a1n 20.000.000 VN\u0110");
                    return res;
                }
            } else if (amount < (long)paymentConfig.getConfig().getMinMoney().intValue()) {
                res.setData("So tien nap nho hon so tien quy dinh");
                return res;
            }
            if ((rechargeDao = new RechargePaygateDaoImpl()).CheckPending(nickname, PAYMENTNAME)) {
                res.setCode(3);
                res.setData("Vui l\u00f2ng ho\u00e0n th\u00e0nh \u0111\u01a1n n\u1ea1p ti\u1ec1n tr\u01b0\u1edbc \u0111\u00f3");
                return res;
            }
            DepositPaygateModel model = new DepositPaygateModel();
            model.Id = "";
            model.Amount = amount;
            model.BankAccountName = "";
            model.BankAccountNumber = "";
            model.BankCode = "";
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
                logger.error("L\u1ed7i t\u1ea1o b\u1ea3n ghi database mongo");
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
        Arrays.sort(signArray);
        StringBuilder resu = new StringBuilder();
        for (String s : signArray) {
            resu.append(s).append("&");
        }
        resu.append("key=").append(this.getPaymentConfig().getConfig().getMerchantKey());
        return PayCommon.getMd5(resu.toString()).toUpperCase();
    }

    private RechargePaywellResponse requestCreateTransaction(String orderId, long amount, String channel, String customerName, String ip) throws Exception {
        RechargePaywellResponse res = new RechargePaywellResponse(1, 0L, 0, 0L, "");
        ZonedDateTime utc = ZonedDateTime.now(ZoneOffset.UTC);
        long timetick = utc.toEpochSecond();
        Config config = this.getPaymentConfig().getConfig();
        String[] paramArray = new String[]{"uid=" + config.getMerchantCode(), "amount=" + amount, "orderid=" + orderId, "channel=" + channel, "notify_url=" + config.getNotifyUrl(), "return_url=" + config.getReturnUrl(), "userip=" + ip, "timestamp=" + timetick, "custom=" + customerName};
        String sign = this.getSignal(paramArray);
        if (sign.isEmpty()) {
            res.setData("sign is empty");
            return res;
        }
        ArrayList<NameValuePair> urlParameters = new ArrayList<NameValuePair>();
        urlParameters.add((NameValuePair)new BasicNameValuePair("uid", config.getMerchantCode()));
        urlParameters.add((NameValuePair)new BasicNameValuePair("orderid", orderId));
        urlParameters.add((NameValuePair)new BasicNameValuePair("amount", String.valueOf(amount)));
        urlParameters.add((NameValuePair)new BasicNameValuePair("channel", channel));
        urlParameters.add((NameValuePair)new BasicNameValuePair("notify_url", config.getNotifyUrl()));
        urlParameters.add((NameValuePair)new BasicNameValuePair("return_url", config.getReturnUrl()));
        urlParameters.add((NameValuePair)new BasicNameValuePair("userip", ip));
        urlParameters.add((NameValuePair)new BasicNameValuePair("timestamp", timetick + ""));
        urlParameters.add((NameValuePair)new BasicNameValuePair("custom", customerName));
        urlParameters.add((NameValuePair)new BasicNameValuePair("sign", sign));
        String result = PayUtils.requestAPI(config.getRequestAPI(), urlParameters);
        logger.info(("princepayResponse: " + result.toString()));
        try {
            JSONObject jsonObj = new JSONObject(result);
            res.setCode(jsonObj.getInt("status"));
            if (jsonObj.getInt("status") != 10000) {
                logger.error(("CreateTransaction PRINCEPAY" + jsonObj.toString()));
                res.setData(jsonObj.getString("sign"));
                RechargePaygateDaoImpl rechargePaygateDao = new RechargePaygateDaoImpl();
                rechargePaygateDao.UpdateStatus(orderId, PayCommon.PAYSTATUS.FAILED.getId(), USERAPPROVE);
                res.setCode(99);
            } else {
                res.setCode(0);
                res.setData(jsonObj.getString("result"));
            }
        }
        catch (Exception e) {
            logger.error(("princepayResponse: " + result.toString()));
        }
        return res;
    }

    private RechargePaywellResponse requestCheckStatusTransaction(String orderId) throws Exception {
        RechargePaywellResponse res = new RechargePaywellResponse(1, 0L, 0, 0L, "");
        try {
            ZonedDateTime utc = ZonedDateTime.now(ZoneOffset.UTC);
            long timetick = utc.toEpochSecond();
            if (this.updateRequestTime(orderId, String.valueOf(timetick), "superadmin").getCode() != 0) {
                res.setData("");
                return res;
            }
            PaymentConfigServiceImpl paymentConfigService = new PaymentConfigServiceImpl();
            PaymentConfig paymentConfig = paymentConfigService.getConfigByKey(PAYMENTNAME);
            Config config = paymentConfig.getConfig();
            String signature_string = "merchantCode=" + config.getMerchantCode() + "&cartId=" + orderId + "&requestTime=" + String.valueOf(timetick);
            String hash = PayCommon.getHMACSHA256(config.getMerchantKey(), signature_string);
            if (hash.isEmpty()) {
                res.setData("");
                return res;
            }
            ArrayList<NameValuePair> urlParameters = new ArrayList<NameValuePair>();
            urlParameters.add((NameValuePair)new BasicNameValuePair("merchantCode", config.getMerchantCode()));
            urlParameters.add((NameValuePair)new BasicNameValuePair("cartId", orderId));
            urlParameters.add((NameValuePair)new BasicNameValuePair("requestTime", String.valueOf(timetick)));
            urlParameters.add((NameValuePair)new BasicNameValuePair("signature", hash));
            String result = PayUtils.requestAPI(config.getStatusAPI(), urlParameters);
            logger.debug(("princepay Response: " + result.toString()));
            try {
                JSONObject jsonObj = new JSONObject(result.toString());
                res.setCode(jsonObj.getInt("code"));
                res.setData(jsonObj.getString("message"));
                Integer code = jsonObj.getInt("code");
                String status = jsonObj.getString("status");
                if (code == 1) {
                    res.setCode(0);
                    ObjectWriter ow = new ObjectMapper().writer().withDefaultPrettyPrinter();
                    String data = ow.writeValueAsString(PayCommon.PAYSTATUS.getByKey(status));
                    res.setData(data);
                }
                return res;
            }
            catch (Exception exception) {
                return res;
            }
        }
        catch (Exception e) {
            logger.debug(e);
            return res;
        }
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    private RechargePaywellResponse addMoney(String orderId, String transId, String nickName, long amount) {
        RechargePaywellResponse res = new RechargePaywellResponse(1, 0L, 0, 0L, "fail");
        HazelcastInstance client = HazelcastClientFactory.getInstance();
        if (client == null) {
            MoneyLogger.log(nickName, PAYMENTNAME, amount, 0L, "vin", "Nap princepay qua ngan hang", "1031", "Cannot connect hazelcast");
            return res;
        }
        IMap userMap = client.getMap("users");
        if (!userMap.containsKey(nickName)) {
            return res;
        }
        try {
            userMap.lock(nickName);
            RechargePaygateDaoImpl rechargeDao = new RechargePaygateDaoImpl();
            DepositPaygateModel depositPayGateModel = rechargeDao.GetById(orderId);
            if (depositPayGateModel == null) {
                RechargePaywellResponse rechargePaywellResponse = res;
                return rechargePaywellResponse;
            }
            UserCacheModel user = (UserCacheModel)userMap.get(nickName);
            long moneyUser = user.getVin();
            long currentMoney = user.getVinTotal();
            long rechargeMoney = user.getRechargeMoney();
            user.setVin(moneyUser += amount);
            user.setVinTotal(currentMoney += amount);
            user.setRechargeMoney(rechargeMoney += amount);
            String desc = "ONLINE".equals(depositPayGateModel.getPayTypeStr()) ? "N\u1ea1p ti\u1ec1n nhanh Princepay" : "N\u1ea1p ti\u1ec1n Princepay";
            MoneyMessageInMinigame messageMoney = new MoneyMessageInMinigame(VinPlayUtils.genMessageId(), user.getId(), nickName, "RechargeByPrincePay", moneyUser, currentMoney, amount, "vin", 0L, 0, 0);
            LogMoneyUserMessage messageLog = new LogMoneyUserMessage(user.getId(), nickName, "RechargeByPrincePay", PAYMENTNAME, currentMoney, amount, "vin", desc, 0L, false, user.isBot());
            messageLog.setReferralCode(user.getReferralCode());
            MessageBusFactory.get("queue_payment").publishOrThrow("queue_payment", messageMoney, 16);
            MessageBusFactory.get("queue_log_money").publish("queue_log_money", messageLog, 601);
            userMap.put(nickName, user);
            res.setCode(0);
            res.setData("");
        }
        catch (Exception e2) {
            logger.debug(e2);
            MoneyLogger.log(nickName, PAYMENTNAME, amount, 0L, "vin", "Nap vin qua princepay", "1031", "rmq error: " + e2.getMessage());
        }
        finally {
            userMap.unlock(nickName);
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
            res = this.requestCreateTransaction(id + "", amount, channel, customerName, ip);
            return res;
        }
        catch (Exception e) {
            logger.debug(e);
            return res;
        }
    }

    @Override
    public RechargePaywellResponse notify(int status, String result, String signature) {
        RechargePaywellResponse res = new RechargePaywellResponse(1, 0L, 0, 0L, "");
        String orderId = "";
        RechargePaygateDaoImpl rechargeDao = new RechargePaygateDaoImpl();
        try {
            Double amount;
            JSONObject json = new JSONObject(result);
            String amountStr = json.getString("amount");
            try {
                amount = Double.parseDouble(amountStr);
            }
            catch (NumberFormatException e) {
                logger.error(e);
                return res;
            }
            String transactionId = json.getString("transactionid");
            orderId = json.getString("orderid");
            DepositPaygateModel model = rechargeDao.GetById(orderId);
            if (model == null) {
                return res;
            }
            if (model.Amount != amount.longValue()) {
                res.setData("Amount wrong ,amount = " + model.Amount);
                return res;
            }
            if (model.Status != PayCommon.PAYSTATUS.PENDING.getId() && model.Status != PayCommon.PAYSTATUS.RECEIVED.getId()) {
                res.setData("status wrong , status = " + model.Status);
                return res;
            }
            String[] paramArray = new String[]{"result=" + result, "status=" + status};
            String signEncode = this.getSignal(paramArray);
            if (!signature.equals(signEncode)) {
                res.setData("Invalid signature");
                return res;
            }
            if (status == 10000) {
                if (!rechargeDao.UpdateStatus(orderId, transactionId, PayCommon.PAYSTATUS.SUCCESS.getId(), USERAPPROVE).booleanValue()) {
                    res.setData("UpdateStatus SUCCESS fail");
                    return res;
                }
                res = this.addMoney(orderId, transactionId, model.Nickname, model.Amount);
                if (res.getCode() != 0) {
                    res.setData("Add money fail");
                    return res;
                }
                if (!rechargeDao.UpdateStatus(orderId, transactionId, PayCommon.PAYSTATUS.COMPLETED.getId(), USERAPPROVE).booleanValue()) {
                    res.setData("UpdateStatus COMPLETED fail");
                    return res;
                }
            } else if (!rechargeDao.UpdateStatus(orderId, transactionId, PayCommon.PAYSTATUS.FAILED.getId(), USERAPPROVE).booleanValue()) {
                res.setData("UpdateStatus FAILED fail");
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
    public RechargePaywellResponse checkStatusTrans(String cartId) {
        RechargePaywellResponse res = new RechargePaywellResponse(1, 0L, 0, 0L, "");
        try {
            if (cartId.isEmpty()) {
                res.setData("Invalid parram(s)");
                return res;
            }
            RechargePaygateDaoImpl rechargeDao = new RechargePaygateDaoImpl();
            DepositPaygateModel model = rechargeDao.GetById(cartId);
            if (model == null) {
                return res;
            }
            return this.requestCheckStatusTransaction(cartId);
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

    public static void main(String[] args) {
        Object[] xxx = new String[]{"amount=200000", "uid=roy88", "orderid=12280869", "channel=908", "notify_url=https://roy88.vip", "return_url=https://roy88.vip", "userip=127.0.0.1", "timestamp=1608823688", "custom=nguyenz"};
        Arrays.sort(xxx);
        String t = "";
        for (Object string : xxx) {
            t = t + (String)string + "&";
        }
        System.out.println(t);
    }
}

