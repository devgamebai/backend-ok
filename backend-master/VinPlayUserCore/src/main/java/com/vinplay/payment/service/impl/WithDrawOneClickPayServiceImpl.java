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
import com.vinplay.payment.dao.impl.WithDrawPaygateDaoImpl;
import com.vinplay.payment.entities.BankConfig;
import com.vinplay.payment.entities.Config;
import com.vinplay.payment.entities.DepositPaygateModel;
import com.vinplay.payment.entities.PaymentConfig;
import com.vinplay.payment.entities.WithDrawPaygateModel;
import com.vinplay.payment.entities.WithDrawPaygateReponse;
import com.vinplay.payment.service.WithDrawOneClickPayService;
import com.vinplay.payment.service.impl.PaymentConfigServiceImpl;
import com.vinplay.payment.utils.PayCommon;
import com.vinplay.payment.utils.PayUtils;
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
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import org.apache.http.NameValuePair;
import org.apache.http.message.BasicNameValuePair;
import org.apache.log4j.Logger;
import org.json.JSONObject;

public class WithDrawOneClickPayServiceImpl
implements WithDrawOneClickPayService {
    private static final Logger logger = Logger.getLogger((String)"backend");
    private static final String PAYMENTNAME = "clickpay";
    private static final String USERAPPROVE = "system";
    private static final String CHANNEL = "WITHDRAWONECLICK";
    private PaymentConfig paymentConfig = null;

    public WithDrawOneClickPayServiceImpl() {
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

    private RechargePaywellResponse updateRequestTime(String orderId, String requesTime, String userApprove) {
        RechargePaywellResponse res = new RechargePaywellResponse(1, 0L, 0, 0L, "");
        try {
            if (orderId.isEmpty() || requesTime.isEmpty() || userApprove.isEmpty()) {
                return res;
            }
            WithDrawPaygateDaoImpl withDrawPaygateDao = new WithDrawPaygateDaoImpl();
            WithDrawPaygateModel model = withDrawPaygateDao.GetById(orderId);
            if (model == null) {
                return res;
            }
            if (!withDrawPaygateDao.UpdateRequestTime(orderId, requesTime, "supadmin").booleanValue()) {
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

    private RechargePaywellResponse requestCreateWithDrawTransaction(WithDrawPaygateModel model, String ip) throws Exception {
        RechargePaywellResponse res = new RechargePaywellResponse(1, 0L, 0, 0L, "");
        ZonedDateTime utc = ZonedDateTime.now(ZoneOffset.UTC);
        long timetick = utc.toEpochSecond();
        if (this.updateRequestTime(model.CartId, String.valueOf(timetick), USERAPPROVE).getCode() != 0) {
            res.setData("");
            return res;
        }
        if (this.paymentConfig == null) {
            this.paymentConfig = this.getPaymentConfig();
        }
        Config config = this.paymentConfig.getConfig();
        String[] paramArray = new String[]{config.getMerchantCode(), model.CartId, model.BankAccountNumber, model.BankCode, String.valueOf(model.Amount)};
        String sign = this.getSignal(paramArray);
        if (sign.isEmpty()) {
            res.setData("");
            return res;
        }
        ArrayList<NameValuePair> urlParameters = new ArrayList<NameValuePair>();
        urlParameters.add((NameValuePair)new BasicNameValuePair("merchant_id", config.getMerchantCode()));
        urlParameters.add((NameValuePair)new BasicNameValuePair("merchant_txn", model.CartId));
        urlParameters.add((NameValuePair)new BasicNameValuePair("merchant_customer", config.getMerchantCode()));
        urlParameters.add((NameValuePair)new BasicNameValuePair("bank_account_no", model.BankAccountNumber));
        urlParameters.add((NameValuePair)new BasicNameValuePair("bank_account_name", model.BankAccountName));
        urlParameters.add((NameValuePair)new BasicNameValuePair("bank_code", model.BankCode));
        urlParameters.add((NameValuePair)new BasicNameValuePair("amount", String.valueOf(model.Amount)));
        urlParameters.add((NameValuePair)new BasicNameValuePair("sign", sign));
        String urlRequest = config.getRequestAPI().replace("payment", "payout").replace("request", "create");
        String result = PayUtils.requestAPI(urlRequest, urlParameters);
        logger.info(("clickpayResponse: " + result.toString()));
        JSONObject jsonObj = new JSONObject(result);
        Integer code = jsonObj.getInt("code");
        if (code != 0) {
            res.setCode(code);
            this.updateStatusWithDraw(model, PayCommon.PAYSTATUS.FAILED, USERAPPROVE);
            res.setData(jsonObj.getString("message"));
        } else {
            res.setCode(code);
            String referenceId = jsonObj.getString("payout_id");
            if (referenceId.isEmpty()) {
                res.setCode(code);
                res = this.updateStatusWithDraw(model, PayCommon.PAYSTATUS.FAILED, USERAPPROVE);
                return res;
            }
            model.ReferenceId = referenceId;
            res = this.updateStatusWithDraw(model, PayCommon.PAYSTATUS.PENDING, USERAPPROVE);
            res.setData(jsonObj.getString("result"));
        }
        return res;
    }

    private RechargePaywellResponse updateStatusWithDraw(WithDrawPaygateModel model, PayCommon.PAYSTATUS status, String userApprove) {
        RechargePaywellResponse res = new RechargePaywellResponse(1, 0L, 0, 0L, "");
        try {
            WithDrawPaygateDaoImpl withDrawPaygateDao = new WithDrawPaygateDaoImpl();
            if (model == null) {
                return res;
            }
            switch (PayCommon.PAYSTATUS.getById(model.Status)) {
                case PENDING: {
                    if (status != PayCommon.PAYSTATUS.RECEIVED) {
                        res.setData("Transaction has rejected");
                        break;
                    }
                    if (!withDrawPaygateDao.UpdateStatus(model.CartId, model.ReferenceId, status.getId(), userApprove.isEmpty() ? USERAPPROVE : userApprove).booleanValue()) break;
                    res.setCode(0);
                    break;
                }
                case RECEIVED: {
                    if (status != PayCommon.PAYSTATUS.FAILED || status != PayCommon.PAYSTATUS.SUCCESS || status != PayCommon.PAYSTATUS.REVIEW || status != PayCommon.PAYSTATUS.SPAM) {
                        res.setData("Transaction has rejected");
                        break;
                    }
                    if (status == PayCommon.PAYSTATUS.SUCCESS) {
                        if (withDrawPaygateDao.UpdateStatus(model.CartId, model.ReferenceId, PayCommon.PAYSTATUS.COMPLETED.getId(), userApprove.isEmpty() ? USERAPPROVE : userApprove).booleanValue()) break;
                        res.setData("Update status COMPLETED failed");
                        break;
                    }
                    if (!withDrawPaygateDao.UpdateStatus(model.CartId, model.ReferenceId, status.getId(), userApprove.isEmpty() ? USERAPPROVE : userApprove).booleanValue()) break;
                    if (status == PayCommon.PAYSTATUS.FAILED) {
                        res = this.addBackMoney(model.CartId, model.Username, model.Amount);
                        break;
                    }
                    res.setCode(0);
                    break;
                }
                case FAILED: {
                    res.setData("Transaction has rejected");
                    break;
                }
                case COMPLETED: {
                    res.setData("Transaction has completed");
                    break;
                }
                case REVIEW: {
                    if (status != PayCommon.PAYSTATUS.COMPLETED || status != PayCommon.PAYSTATUS.FAILED || status != PayCommon.PAYSTATUS.SPAM) {
                        res.setData("Invalid status");
                        break;
                    }
                    if (!withDrawPaygateDao.UpdateStatus(model.CartId, model.ReferenceId, status.getId(), userApprove.isEmpty() ? USERAPPROVE : userApprove).booleanValue()) break;
                    res.setCode(0);
                    break;
                }
                case SPAM: {
                    res.setData("Transaction request many times");
                    break;
                }
                default: {
                    res.setData("Invalid status");
                }
            }
            return res;
        }
        catch (Exception e) {
            logger.debug(e);
            return res;
        }
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    private RechargePaywellResponse discountMoney(String orderId, String nickname, long amount) {
        RechargePaywellResponse res = new RechargePaywellResponse(1, 0L, 0, 0L, "fail");
        HazelcastInstance client = HazelcastClientFactory.getInstance();
        if (client == null) {
            MoneyLogger.log(nickname, "CashOutByClickPay", amount, 0L, "vin", "Cashout qua clickpay fail", "1031", "Cannot connect hazelcast");
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
            MoneyMessageInMinigame messageMoney = new MoneyMessageInMinigame(VinPlayUtils.genMessageId(), user.getId(), nickname, "CashOutByClickPay", moneyUser, currentMoney, amount, "vin", 0L, 0, 0);
            LogMoneyUserMessage messageLog = new LogMoneyUserMessage(user.getId(), nickname, "CashOutByClickPay", "Cashout", currentMoney, amount * -1L, "vin", desc, 0L, false, user.isBot());
            messageLog.setReferralCode(user.getReferralCode());
            MessageBusFactory.get("queue_payment").publishOrThrow("queue_payment", messageMoney, 16);
            MessageBusFactory.get("queue_log_money").publish("queue_log_money", messageLog, 601);
            userMap.put(nickname, user);
            res.setCode(0);
            res.setData("");
        }
        catch (Exception e2) {
            logger.debug(e2);
            MoneyLogger.log(username, "CashOutByClickPay", amount, 0L, "vin", "Cashout qua clickpay fail", "1031", "rmq error: " + e2.getMessage());
        }
        finally {
            userMap.unlock(nickname);
        }
        return res;
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    private RechargePaywellResponse addBackMoney(String orderId, String nickname, long amount) {
        RechargePaywellResponse res = new RechargePaywellResponse(1, 0L, 0, 0L, "fail");
        String username = "";
        HazelcastInstance client = HazelcastClientFactory.getInstance();
        if (client == null) {
            MoneyLogger.log(nickname, "CashOutByClickPay", amount, 0L, "vin", "Nap tien do cashout fail qua clickpay fail", "1031", "Cannot connect hazelcast");
            return res;
        }
        IMap userMap = client.getMap("users");
        if (!userMap.containsKey(nickname)) {
            return res;
        }
        try {
            userMap.lock(nickname);
            WithDrawPaygateDaoImpl rechargeDao = new WithDrawPaygateDaoImpl();
            WithDrawPaygateModel withdrawModel = rechargeDao.GetById(orderId);
            if (withdrawModel == null) {
                RechargePaywellResponse rechargePaywellResponse = res;
                return rechargePaywellResponse;
            }
            UserCacheModel user = (UserCacheModel)userMap.get(nickname);
            username = user.getUsername();
            long moneyUser = user.getVin();
            long currentMoney = user.getVinTotal();
            long rechargeMoney = user.getRechargeMoney();
            user.setVin(moneyUser += amount);
            user.setVinTotal(currentMoney += amount);
            user.setRechargeMoney(rechargeMoney += amount);
            String desc = "Ho\u00e0n tr\u1ea3 clickpay";
            MoneyMessageInMinigame messageMoney = new MoneyMessageInMinigame(VinPlayUtils.genMessageId(), user.getId(), nickname, "CashOutByClickPay", moneyUser, currentMoney, amount, "vin", 0L, 0, 0);
            LogMoneyUserMessage messageLog = new LogMoneyUserMessage(user.getId(), nickname, "CashOutByClickPay", "Cashout qua clickpay fail", currentMoney, amount, "vin", desc, 0L, false, user.isBot());
            messageLog.setReferralCode(user.getReferralCode());
            MessageBusFactory.get("queue_payment").publishOrThrow("queue_payment", messageMoney, 16);
            MessageBusFactory.get("queue_log_money").publish("queue_log_money", messageLog, 601);
            userMap.put(nickname, user);
            res.setCode(0);
            res.setData("");
        }
        catch (Exception e2) {
            logger.debug(e2);
            MoneyLogger.log(username, "CashOutByClickPay", amount, 0L, "vin", "Cashout qua clickpay fail", "1031", "rmq error: " + e2.getMessage());
        }
        finally {
            userMap.unlock(nickname);
        }
        return res;
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    private RechargePaywellResponse refundMoney(String orderId, String nickname, long amount, String reason) {
        RechargePaywellResponse res = new RechargePaywellResponse(1, 0L, 0, 0L, "fail");
        String username = "";
        HazelcastInstance client = HazelcastClientFactory.getInstance();
        if (client == null) {
            MoneyLogger.log(nickname, "RefundRecharge", amount, 0L, "vin", "Ho\u00e0n tr\u1ea3 , l\u00fd do " + reason, "1031", "Cannot connect hazelcast");
            return res;
        }
        IMap userMap = client.getMap("users");
        if (!userMap.containsKey(nickname)) {
            return res;
        }
        try {
            userMap.lock(nickname);
            WithDrawPaygateDaoImpl rechargeDao = new WithDrawPaygateDaoImpl();
            WithDrawPaygateModel withdrawModel = rechargeDao.GetById(orderId);
            if (withdrawModel == null) {
                RechargePaywellResponse rechargePaywellResponse = res;
                return rechargePaywellResponse;
            }
            UserCacheModel user = (UserCacheModel)userMap.get(nickname);
            username = user.getUsername();
            long moneyUser = user.getVin();
            long currentMoney = user.getVinTotal();
            long rechargeMoney = user.getRechargeMoney();
            user.setVin(moneyUser += amount);
            user.setVinTotal(currentMoney += amount);
            user.setRechargeMoney(rechargeMoney += amount);
            String desc = "T\u1eeb ch\u1ed1i r\u00fat ti\u1ec1n  , l\u00fd do :" + reason;
            MoneyMessageInMinigame messageMoney = new MoneyMessageInMinigame(VinPlayUtils.genMessageId(), user.getId(), nickname, "RefundRecharge", moneyUser, currentMoney, amount, "vin", 0L, 0, 0);
            LogMoneyUserMessage messageLog = new LogMoneyUserMessage(user.getId(), nickname, "RefundRecharge", "REJECT_CASHOUT", currentMoney, amount, "vin", desc, 0L, false, user.isBot());
            messageLog.setReferralCode(user.getReferralCode());
            MessageBusFactory.get("queue_payment").publishOrThrow("queue_payment", messageMoney, 16);
            MessageBusFactory.get("queue_log_money").publish("queue_log_money", messageLog, 601);
            userMap.put(nickname, user);
            res.setCode(0);
            res.setData("");
        }
        catch (Exception e2) {
            logger.debug(e2);
            MoneyLogger.log(username, "RefundRecharge", amount, 0L, "vin", "Ho\u00e0n tr\u1ea3 , l\u00fd do " + reason, "1031", "rmq error: " + e2.getMessage());
        }
        finally {
            userMap.unlock(nickname);
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
            urlParameters.add((NameValuePair)new BasicNameValuePair("merchant_txn", orderId));
            urlParameters.add((NameValuePair)new BasicNameValuePair("sign", sign));
            String url = config.getStatusAPI().replace("payment", "payout");
            String result = PayUtils.requestAPI(url, urlParameters);
            logger.debug(("clickpay Response: " + result.toString()));
            try {
                JSONObject jsonObj = new JSONObject(result.toString());
                res.setCode(jsonObj.getInt("code"));
                Integer code = jsonObj.getInt("code");
                if (code == 0) {
                    res.setCode(code);
                    String data = jsonObj.getString("data");
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

    @Override
    public RechargePaywellResponse withdrawal(String orderId, String approvedName, String ip) {
        RechargePaywellResponse res = new RechargePaywellResponse(1, 0L, 0, 0L, "");
        try {
            WithDrawPaygateModel model = new WithDrawPaygateModel();
            WithDrawPaygateDaoImpl withdrawDao = new WithDrawPaygateDaoImpl();
            model = withdrawDao.GetByOrderId(orderId);
            if (model == null) {
                res.setData("Object does not exist");
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
                bankCode = ((BankConfig)paymentConfig.getConfig().getBanks().stream().filter(item -> item.getName().equals(bankName)).findFirst().orElse(null)).getKey();
            }
            catch (Exception exception) {
                // empty catch block
            }
            if (!withdrawDao.UpdateInfo(orderId, paymentConfig.getConfig().getMerchantCode(), CHANNEL, PAYMENTNAME, bankCode, approvedName).booleanValue()) {
                return res;
            }
            model = withdrawDao.GetByOrderId(orderId);
            if (model == null) {
                res.setData("Object does not exist");
                return res;
            }
            res = this.requestCreateWithDrawTransaction(model, ip);
            return res;
        }
        catch (Exception e) {
            logger.debug(e);
            return res;
        }
    }

    @Override
    public boolean notify(WithDrawPaygateModel model, int status) {
        RechargePaywellResponse res = new RechargePaywellResponse(1, 0L, 0, 0L, "");
        try {
            res = this.updateStatusWithDraw(model, PayCommon.PAYSTATUS.getById(status), USERAPPROVE);
            return res.getCode() == 0;
        }
        catch (Exception e) {
            logger.debug(e);
            return false;
        }
    }

    @Override
    public RechargePaywellResponse checkStatus(String orderId) {
        RechargePaywellResponse res = new RechargePaywellResponse(1, 0L, 0, 0L, "");
        try {
            if (orderId == null || orderId.isEmpty()) {
                res.setData("Invalid parram(s)");
                return res;
            }
            RechargePaywellResponse resResult = this.requestCheckStatusTransaction(orderId);
            if (res.getCode() == 0) {
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
            RechargePaygateDaoImpl rechargeDao = new RechargePaygateDaoImpl();
            DepositPaygateModel model = rechargeDao.GetById(orderId);
            if (model == null) {
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

    @Override
    public boolean reject(String orderId, String approvedName, String reason) {
        try {
            WithDrawPaygateDaoImpl withDrawDao = new WithDrawPaygateDaoImpl();
            WithDrawPaygateModel model = withDrawDao.GetById(orderId);
            if (model == null) {
                logger.error(("[NOTIFY_WITHDRAW] orderId is not exist , orderid=" + orderId));
                return false;
            }
            if (model.Status != PayCommon.PAYSTATUS.REQUEST.getId() && model.Status != PayCommon.PAYSTATUS.PENDING.getId() && model.Status != PayCommon.PAYSTATUS.RECEIVED.getId()) {
                System.out.println(model.Status);
                System.out.println(PayCommon.PAYSTATUS.REQUEST.getId());
                logger.error(("[NOTIFY_WITHDRAW] reject status " + (PayCommon.PAYSTATUS.getById(model.Status))));
                return false;
            }
            String desc = "T\u1eeb ch\u1ed1i r\u00fat ti\u1ec1n  , l\u00fd do :" + reason;
            if (!withDrawDao.UpdateStatus(orderId, model.ReferenceId, PayCommon.PAYSTATUS.FAILED.getId(), approvedName, desc).booleanValue()) {
                logger.error(("[NOTIFY_WITHDRAW] update status = FAILED fail, orderid=" + orderId));
                return false;
            }
            RechargePaywellResponse res = this.refundMoney(orderId, model.Nickname, model.Amount, reason);
            if (res.getCode() != 0) {
                TelegramAlert.sendMessage("[NOTIFY_WITHDRAW] NOTIFY status =" + PayCommon.PAYSTATUS.FAILED.getId() + " . Vui long kiem tra order_id tren he thong  , order_id =" + orderId);
                return false;
            }
            return true;
        }
        catch (Exception e) {
            logger.error(e);
            return false;
        }
    }

    @Override
    public Map<String, Object> FindTransaction(String nickname, int status, int page, int maxItem, String fromTime, String endTime, String providerName) {
        Map<String, Object> data = new HashMap();
        try {
            WithDrawPaygateDaoImpl dao = new WithDrawPaygateDaoImpl();
            data = dao.FindTransaction(nickname, status, page, maxItem, fromTime, endTime, providerName);
            return data;
        }
        catch (Exception e) {
            logger.debug(e);
            return new HashMap<String, Object>();
        }
    }
}

