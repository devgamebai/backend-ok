/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.fasterxml.jackson.databind.ObjectMapper
 *  com.fasterxml.jackson.databind.ObjectWriter
 *  com.hazelcast.core.HazelcastInstance
 *  com.hazelcast.core.IMap
 *  org.apache.commons.lang3.StringUtils
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
import com.vinplay.payment.dao.impl.WithDrawPaygateDaoImpl;
import com.vinplay.payment.entities.BankConfig;
import com.vinplay.payment.entities.Config;
import com.vinplay.payment.entities.PaymentConfig;
import com.vinplay.payment.entities.UserWithdraw;
import com.vinplay.payment.entities.WithDrawPaygateModel;
import com.vinplay.payment.entities.WithDrawPaygateReponse;
import com.vinplay.payment.service.WithDrawPrincePayService;
import com.vinplay.payment.service.impl.PaymentConfigServiceImpl;
import com.vinplay.payment.utils.PayCommon;
import com.vinplay.payment.utils.PayUtils;
import com.vinplay.payment.utils.PaymentConstant;
import com.vinplay.usercore.logger.MoneyLogger;
import com.vinplay.utils.TelegramAlert;
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
import java.util.Date;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.NameValuePair;
import org.apache.http.message.BasicNameValuePair;
import org.apache.log4j.Logger;
import org.json.JSONObject;

public class WithDrawPrincePayServiceImpl
implements WithDrawPrincePayService {
    private static final String NOTIFY_WITHDRAW_URL = "https://iwspay.roy88.vip/payprince/withdraw/notify";
    private static final Logger logger = Logger.getLogger((String)"backend");
    private static final String PAYMENTNAME = "princepay";

    private PaymentConfig getPaymentConfig() {
        PaymentConfigServiceImpl paymentConfigService = new PaymentConfigServiceImpl();
        return paymentConfigService.getConfigByKey(PAYMENTNAME);
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

    private RechargePaywellResponse requestCreateWithDrawTransaction(WithDrawPaygateModel model, String ip) throws Exception {
        String[] paramArray;
        String sign;
        RechargePaywellResponse res = new RechargePaywellResponse(1, 0L, 0, 0L, "");
        ZonedDateTime utc = ZonedDateTime.now(ZoneOffset.UTC);
        long timetick = utc.toEpochSecond();
        Config config = this.getPaymentConfig().getConfig();
        String notifyUrlWithdraw = config.getNotifyWithdrawUrl();
        if (notifyUrlWithdraw == null || "".equals(notifyUrlWithdraw)) {
            notifyUrlWithdraw = NOTIFY_WITHDRAW_URL;
        }
        if ((sign = this.getSignal(paramArray = new String[]{"uid=" + config.getMerchantCode(), "orderid=" + model.CartId, "channel=" + model.PaymentType, "notify_url=" + notifyUrlWithdraw, "amount=" + model.Amount, "userip=" + ip, "timestamp=" + timetick, "custom=" + model.BankAccountName, "bank_account=" + model.BankAccountName, "bank_no=" + model.BankAccountNumber, "bank_id=" + model.BankCode, "bank_province=" + model.BankBranch, "bank_city=VN", "bank_sub=" + model.BankBranch, "user_name="})).isEmpty()) {
            res.setData("Sai ch\u1eef k\u00fd");
            return res;
        }
        ArrayList<NameValuePair> urlParameters = new ArrayList<NameValuePair>();
        urlParameters.add((NameValuePair)new BasicNameValuePair("uid", config.getMerchantCode()));
        urlParameters.add((NameValuePair)new BasicNameValuePair("orderid", model.CartId));
        urlParameters.add((NameValuePair)new BasicNameValuePair("channel", model.PaymentType));
        urlParameters.add((NameValuePair)new BasicNameValuePair("notify_url", notifyUrlWithdraw));
        urlParameters.add((NameValuePair)new BasicNameValuePair("amount", String.valueOf(model.Amount)));
        urlParameters.add((NameValuePair)new BasicNameValuePair("userip", ip));
        urlParameters.add((NameValuePair)new BasicNameValuePair("timestamp", timetick + ""));
        urlParameters.add((NameValuePair)new BasicNameValuePair("custom", model.BankAccountName));
        urlParameters.add((NameValuePair)new BasicNameValuePair("sign", sign));
        urlParameters.add((NameValuePair)new BasicNameValuePair("bank_account", model.BankAccountName));
        urlParameters.add((NameValuePair)new BasicNameValuePair("bank_no", model.BankAccountNumber));
        urlParameters.add((NameValuePair)new BasicNameValuePair("bank_id", model.BankCode));
        urlParameters.add((NameValuePair)new BasicNameValuePair("bank_province", model.BankBranch));
        urlParameters.add((NameValuePair)new BasicNameValuePair("bank_city", "VN"));
        urlParameters.add((NameValuePair)new BasicNameValuePair("bank_sub", model.BankBranch));
        urlParameters.add((NameValuePair)new BasicNameValuePair("user_name", ""));
        logger.info(("princepayRequest: " + (urlParameters).toString()));
        String url = "https://api.princepay.support/applyfor";
        String result = PayUtils.requestAPI(url, urlParameters);
        logger.info(("princepayResponse: " + result.toString()));
        JSONObject jsonObj = new JSONObject(result);
        WithDrawPaygateDaoImpl withDrawPaygateDao = new WithDrawPaygateDaoImpl();
        if (jsonObj.getInt("status") != 10000) {
            if (jsonObj.getInt("status") == 30020) {
                res.setCode(jsonObj.getInt("status"));
                res.setData("V\u00ed c\u1ed5ng thanh to\u00e1n PRINCEPAY h\u1ebft ti\u1ec1n !");
                TelegramAlert.sendMessage("V\u00ed c\u1ed5ng thanh to\u00e1n PRINCEPAY h\u1ebft ti\u1ec1n !");
            } else {
                res.setCode(jsonObj.getInt("status"));
                withDrawPaygateDao.UpdateStatus(model.CartId, PayCommon.PAYSTATUS.FAILED.getId(), model.UserApprove);
                res.setData(jsonObj.getString("result"));
                TelegramAlert.sendMessage("[NOTIFY_WITHDRAW] NOTIFY status =" + PayCommon.PAYSTATUS.FAILED.getId() + " . Vui l\u00f2ng ki\u1ec3m tra order_id tr\u00ean princepay v\u00e0 c\u1ed9ng ti\u1ec1n th\u1ee7 c\u00f4ng l\u1ea1i cho kh\u00e1ch , order_id =" + model.CartId);
            }
        } else {
            String transactionid;
            JSONObject jsonObjResult = new JSONObject(jsonObj.getString("result"));
            try {
                transactionid = jsonObjResult.getString("transactionid");
            }
            catch (Exception e) {
                transactionid = "";
            }
            if (transactionid == null || transactionid.isEmpty()) {
                withDrawPaygateDao.UpdateStatus(model.CartId, PayCommon.PAYSTATUS.FAILED.getId(), model.UserApprove);
                res.setData(jsonObj.getString("result"));
                TelegramAlert.sendMessage("[NOTIFY_WITHDRAW] Kh\u00f4ng th\u1ec3 l\u1ea5y \u0111c transactionid tr\u00ean princepay . Status =" + PayCommon.PAYSTATUS.FAILED.getId() + " . Vui l\u00f2ng ki\u1ec3m tra order_id tr\u00ean princepay  , order_id =" + model.CartId);
                return res;
            }
            res.setCode(0);
            withDrawPaygateDao.UpdateStatus(model.CartId, transactionid, PayCommon.PAYSTATUS.PENDING.getId(), model.UserApprove);
            res.setData(jsonObj.getString("result"));
        }
        return res;
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    private RechargePaywellResponse discountMoney(String orderId, String nickname, long amount) {
        RechargePaywellResponse res = new RechargePaywellResponse(1, 0L, 0, 0L, "fail");
        HazelcastInstance client = HazelcastClientFactory.getInstance();
        if (client == null) {
            MoneyLogger.log(nickname, "REQUEST_CASHOUT", amount, 0L, "vin", "Y\u00eau c\u1ea7u r\u00fat ti\u1ec1n", "1031", "Cannot connect hazelcast");
            return res;
        }
        String username = "";
        IMap userMap = client.getMap("users");
        if (!userMap.containsKey(nickname)) {
            return res;
        }
        try {
            userMap.lock(nickname);
            WithDrawPaygateDaoImpl rechargeDao = new WithDrawPaygateDaoImpl();
            WithDrawPaygateModel model = rechargeDao.GetById(orderId);
            if (model == null) {
                RechargePaywellResponse rechargePaywellResponse = res;
                return rechargePaywellResponse;
            }
            UserCacheModel user = (UserCacheModel)userMap.get(nickname);
            username = user.getUsername();
            long moneyUser = user.getVin();
            long currentMoney = user.getVinTotal();
            int cashoutMoney = user.getCashout();
            if (moneyUser < amount || currentMoney < amount) {
                res.setData("Tai khoan khong du tien");
                RechargePaywellResponse rechargePaywellResponse = res;
                return rechargePaywellResponse;
            }
            user.setVin(moneyUser -= amount);
            user.setVinTotal(currentMoney -= amount);
            user.setCashout(cashoutMoney += (int)amount);
            user.setCashoutTime(new Date());
            String desc = "R\u00fat ti\u1ec1n v\u1ec1 STK: " + model.BankAccountNumber;
            MoneyMessageInMinigame messageMoney = new MoneyMessageInMinigame(VinPlayUtils.genMessageId(), user.getId(), nickname, "REQUEST_CASHOUT", moneyUser, currentMoney, amount, "vin", 0L, 0, 0);
            LogMoneyUserMessage messageLog = new LogMoneyUserMessage(user.getId(), nickname, "REQUEST_CASHOUT", "Cashout", currentMoney, amount * -1L, "vin", desc, 0L, false, user.isBot());
            messageLog.setReferralCode(user.getReferralCode());
            MessageBusFactory.get("queue_payment").publishOrThrow("queue_payment", messageMoney, 16);
            MessageBusFactory.get("queue_log_money").publish("queue_log_money", messageLog, 601);
            userMap.put(nickname, user);
            res.setCode(0);
            res.setData("");
            res.setCurrentMoney(currentMoney);
        }
        catch (Exception e2) {
            logger.debug(e2);
            MoneyLogger.log(username, "REQUEST_CASHOUT", amount, 0L, "vin", "Y\u00eau c\u1ea7u r\u00fat ti\u1ec1n", "1031", "rmq error: " + e2.getMessage());
        }
        finally {
            userMap.unlock(nickname);
        }
        return res;
    }

    @Override
    public RechargePaywellResponse requestWithdrawUser(String userId, String username, String nickname, long amount, String bankNumber) {
        RechargePaywellResponse res = new RechargePaywellResponse(1, 0L, 0, 0L, "");
        RechargePaygateDaoImpl depositPaygate = new RechargePaygateDaoImpl();
        WithDrawPaygateDaoImpl withdrawDao = new WithDrawPaygateDaoImpl();
        if (withdrawDao.countNumberWithdrawSuccessInDay(nickname) >= 5L) {
            res.setCode(89);
            return res;
        }
        try {
            HazelcastInstance client = HazelcastClientFactory.getInstance();
            if (client == null) {
                MoneyLogger.log(username, "REQUEST_WITHDRAW", amount, 0L, "vin", "Request withdraw ", "1031", "Cannot connect hazelcast");
                return res;
            }
            IMap userMap = client.getMap("users");
            if (!userMap.containsKey(nickname)) {
                return res;
            }
            UserCacheModel user = (UserCacheModel)userMap.get(nickname);
            long moneyUser = user.getVin();
            long currentMoney = user.getVinTotal();
            if (moneyUser < amount || currentMoney < amount) {
                res.setData("Tai khoan khong du tien");
                return res;
            }
            String[] strings = bankNumber.split("\\|");
            WithDrawPaygateModel model = new WithDrawPaygateModel();
            String bankName = strings[0];
            model.Id = "";
            model.Amount = amount;
            model.BankAccountName = strings[2];
            model.BankAccountNumber = strings[1];
            model.BankCode = bankName;
            model.BankName = bankName;
            model.BankBranch = "";
            model.CartId = "";
            model.CreatedAt = "";
            model.IsDeleted = false;
            model.PaymentType = "";
            model.MerchantCode = "";
            model.ProviderName = "";
            model.ModifiedAt = "";
            model.UserId = userId;
            model.Username = username;
            model.Nickname = nickname;
            model.ReferenceId = "";
            model.RequestTime = "";
            model.Status = PayCommon.PAYSTATUS.REQUEST.getId();
            model.UserApprove = username;
            long id = withdrawDao.Add(model);
            if (id == 0L) {
                return res;
            }
            model = withdrawDao.GetById(String.valueOf(id));
            res = this.discountMoney(model.CartId, model.Nickname, model.Amount);
            if (res.getCode() != 0) {
                return res;
            }
            UserWithdraw userWithdraw = new UserWithdraw(model.Nickname, model.Amount, model.BankAccountNumber, model.BankAccountName, strings[0]);
            TelegramAlert.SendMessageCashout(userWithdraw);
            res.setCode(0);
            res.setTid(String.valueOf(id));
            return res;
        }
        catch (Exception e) {
            return null;
        }
    }

    @Override
    public RechargePaywellResponse withdrawal(String orderId, String channel, String approvedName, String ip) {
        RechargePaywellResponse res = new RechargePaywellResponse(1, 0L, 0, 0L, "");
        try {
            WithDrawPaygateDaoImpl withdrawDao = new WithDrawPaygateDaoImpl();
            WithDrawPaygateModel model = withdrawDao.GetByOrderId(orderId);
            channel = PayUtils.getPayType(PaymentConstant.PayType.WITHDRAW.getKey(), PAYMENTNAME);
            if (model == null) {
                res.setData("Object does not exist");
                return res;
            }
            if (PayCommon.PAYSTATUS.REQUEST.getId() != model.Status) {
                res.setData("status wrong , status = " + model.Status);
                return res;
            }
            PaymentConfigServiceImpl paymentConfigService = new PaymentConfigServiceImpl();
            PaymentConfig paymentConfig = paymentConfigService.getConfigByKey(PAYMENTNAME);
            if (model.Amount < (long)paymentConfig.getConfig().getMinMoney().intValue()) {
                res.setData("So tien rut nho hon so tien quy dinh");
                return res;
            }
            String bankName = model.BankCode;
            String bankCode = "";
            try {
                bankCode = ((BankConfig)paymentConfig.getConfig().getBanks().stream().filter(item -> item.getName().equalsIgnoreCase(bankName)).findFirst().orElse(null)).getKey();
            }
            catch (Exception exception) {
                // empty catch block
            }
            if (StringUtils.isBlank((CharSequence)bankCode)) {
                res.setData("Kh\u00f4ng c\u00f3 m\u00e3 ng\u00e2n h\u00e0ng backcode ph\u00f9 h\u1ee3p , bankName=" + bankName);
                return res;
            }
            String merchantCode = paymentConfig.getConfig().getMerchantCode();
            if (!withdrawDao.UpdateInfo(orderId, merchantCode, channel, PAYMENTNAME, bankCode, approvedName).booleanValue()) {
                return res;
            }
            model.MerchantCode = merchantCode;
            model.PaymentType = channel;
            model.ProviderName = PAYMENTNAME;
            model.UserApprove = approvedName;
            model.BankCode = bankCode;
            model.Status = PayCommon.PAYSTATUS.PENDING.getId();
            model.ModifiedAt = VinPlayUtils.getCurrentDateTime();
            return this.requestCreateWithDrawTransaction(model, ip);
        }
        catch (Exception e) {
            logger.error(e);
            return res;
        }
    }

    @Override
    public boolean notify(int status, String result, String sign) {
        String orderId = "";
        String custom = "";
        try {
            Double amount;
            JSONObject json = new JSONObject(result);
            String amountStr = json.getString("amount");
            String realAmountStr = json.getString("real_amount");
            try {
                amount = Double.parseDouble(amountStr);
            }
            catch (NumberFormatException e) {
                logger.error(e);
                return false;
            }
            String transactionId = json.getString("transactionid");
            orderId = json.getString("orderid");
            custom = json.getString("custom");
            WithDrawPaygateDaoImpl withDrawDao = new WithDrawPaygateDaoImpl();
            WithDrawPaygateModel model = withDrawDao.GetById(orderId);
            if (model == null) {
                logger.error(("[NOTIFY_WITHDRAW] orderId is not exist , orderid=" + orderId));
                return false;
            }
            if (model.Amount != amount.longValue()) {
                logger.error(("[NOTIFY_WITHDRAW]  amount request is incorrect , orderid=" + orderId));
                return false;
            }
            if (model.Status != PayCommon.PAYSTATUS.PENDING.getId()) {
                logger.error(("[NOTIFY_WITHDRAW]  status is wrong , orderid=" + orderId));
                return false;
            }
            String[] paramArray = new String[]{"result=" + result, "status=" + status};
            String signEncode = this.getSignal(paramArray);
            if (!sign.equals(signEncode)) {
                logger.error(("[NOTIFY_WITHDRAW]  sign is invalid , orderid=" + orderId));
                return false;
            }
            if (status == 10000) {
                if (!withDrawDao.UpdateStatus(orderId, transactionId, PayCommon.PAYSTATUS.COMPLETED.getId(), model.UserApprove).booleanValue()) {
                    logger.error(("[NOTIFY_WITHDRAW] update status = COMPLETED fail, orderid=" + orderId));
                    return false;
                }
                return true;
            }
            if (!withDrawDao.UpdateStatus(orderId, transactionId, PayCommon.PAYSTATUS.FAILED.getId(), model.UserApprove).booleanValue()) {
                logger.error(("[NOTIFY_WITHDRAW] update status = FAILED fail, orderid=" + orderId));
            }
            TelegramAlert.sendMessage("[NOTIFY_WITHDRAW] NOTIFY status =" + status + " . Vui l\u00f2ng ki\u1ec3m tra order_id tr\u00ean princepay v\u00e0 c\u1ed9ng ti\u1ec1n th\u1ee7 c\u00f4ng l\u1ea1i cho kh\u00e1ch , order_id =" + orderId);
            return false;
        }
        catch (Exception e) {
            logger.error(e);
            return false;
        }
    }

    @Override
    public RechargePaywellResponse findWithDraw(String nickname, int status, int page, int maxItem, String fromTime, String endTime, String providerName) {
        RechargePaywellResponse res = new RechargePaywellResponse(1, 0L, 0, 0L, "");
        try {
            WithDrawPaygateDaoImpl withDrawDao = new WithDrawPaygateDaoImpl();
            WithDrawPaygateReponse withDrawPayWellReponses = withDrawDao.Find(nickname, status, page, maxItem, fromTime, endTime, providerName);
            ObjectWriter ow = new ObjectMapper().writer().withDefaultPrettyPrinter();
            String json = ow.writeValueAsString(withDrawPayWellReponses);
            res.setCode(0);
            res.setData(json.toString());
            return res;
        }
        catch (Exception e) {
            logger.debug(e);
            return res;
        }
    }
}

