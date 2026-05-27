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
import com.vinplay.payment.entities.Config;
import com.vinplay.payment.entities.DepositPaygateModel;
import com.vinplay.payment.entities.DepositPaygateReponse;
import com.vinplay.payment.entities.PaymentConfig;
import com.vinplay.payment.entities.PaywellNotifyRequest;
import com.vinplay.payment.service.RechargePayWellService;
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
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.NameValuePair;
import org.apache.http.message.BasicNameValuePair;
import org.apache.log4j.Logger;
import org.json.JSONObject;

public class RechargePayWellServiceImpl
implements RechargePayWellService {
    private static final Logger logger = Logger.getLogger((String)"RechargePayWell");
    private static final String PAYMENTNAME = "paywell";
    private static final String USERAPPROVE = "system";
    private static final List<Integer> RIGHT_STATUS = Arrays.asList(0, 1, 2, 3, 4, 11);

    private PaymentConfig getPaymentConfig() {
        PaymentConfigServiceImpl paymentConfigService = new PaymentConfigServiceImpl();
        return paymentConfigService.getConfigByKey(PAYMENTNAME);
    }

    private RechargePaywellResponse createOrder(String userId, String username, String nickname, long amount, String bankCode, String paymentType) {
        try {
            RechargePaywellResponse res = new RechargePaywellResponse(1, 0L, 0, 0L, "");
            if (nickname.isEmpty() || amount <= 0L || bankCode.isEmpty()) {
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
            model.RequestTime = "";
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
            DepositPaygateModel model = rechargeDao.GetByOrderId(orderId);
            if (model == null) {
                return res;
            }
            if (!rechargeDao.UpdateRequestTime(orderId, requesTime, userApprove.isEmpty() ? USERAPPROVE : userApprove).booleanValue()) {
                return res;
            }
            res.setCode(0);
            return res;
        }
        catch (Exception e) {
            return res;
        }
    }

    private RechargePaywellResponse requestCreateTransaction(String orderId, long amount, String bankCode, String payType, String customerId, String customerFullName) throws Exception {
        RechargePaywellResponse res = new RechargePaywellResponse(1, 0L, 0, 0L, "");
        ZonedDateTime utc = ZonedDateTime.now(ZoneOffset.UTC);
        long timetick = utc.toEpochSecond();
        if (this.updateRequestTime(orderId, String.valueOf(timetick), USERAPPROVE).getCode() != 0) {
            res.setData("");
            return res;
        }
        PaymentConfigServiceImpl paymentConfigService = new PaymentConfigServiceImpl();
        PaymentConfig paymentConfig = paymentConfigService.getConfigByKey(PAYMENTNAME);
        Config config = paymentConfig.getConfig();
        String signature_string = "merchantCode=" + config.getMerchantCode() + "&cartId=" + orderId + "&amount=" + amount + "&currencyCode=" + config.getCurrencyCode() + "&payType=" + payType + "&bankCode=" + bankCode + "&returnUrl=" + config.getReturnUrl() + "&notifyUrl=" + config.getNotifyUrl() + "&customerId=" + customerId + "&requestTime=" + String.valueOf(timetick);
        String hash = PayCommon.getHMACSHA256(config.getMerchantKey(), signature_string);
        if (hash.isEmpty()) {
            res.setData("");
            return res;
        }
        ArrayList<NameValuePair> urlParameters = new ArrayList<NameValuePair>();
        urlParameters.add((NameValuePair)new BasicNameValuePair("merchantCode", config.getMerchantCode()));
        urlParameters.add((NameValuePair)new BasicNameValuePair("cartId", orderId));
        urlParameters.add((NameValuePair)new BasicNameValuePair("amount", String.valueOf(amount)));
        urlParameters.add((NameValuePair)new BasicNameValuePair("currencyCode", config.getCurrencyCode()));
        urlParameters.add((NameValuePair)new BasicNameValuePair("payType", payType));
        urlParameters.add((NameValuePair)new BasicNameValuePair("bankCode", bankCode));
        urlParameters.add((NameValuePair)new BasicNameValuePair("returnUrl", config.getReturnUrl()));
        urlParameters.add((NameValuePair)new BasicNameValuePair("notifyUrl", config.getNotifyUrl()));
        urlParameters.add((NameValuePair)new BasicNameValuePair("customerId", customerId));
        urlParameters.add((NameValuePair)new BasicNameValuePair("customerFullName", customerFullName));
        urlParameters.add((NameValuePair)new BasicNameValuePair("requestTime", String.valueOf(timetick)));
        urlParameters.add((NameValuePair)new BasicNameValuePair("signature", hash));
        String result = PayUtils.requestAPI(config.getRequestAPI(), urlParameters);
        logger.info(("paywellResponse: " + result.toString()));
        try {
            JSONObject jsonObj = new JSONObject(result);
            res.setCode(jsonObj.getInt("code"));
            if (jsonObj.getInt("code") != 1) {
                if (jsonObj.getInt("code") == 20) {
                    res.setData(jsonObj.getString("message"));
                    RechargePaygateDaoImpl rechargePaygateDao = new RechargePaygateDaoImpl();
                    rechargePaygateDao.UpdateStatus(orderId, PayCommon.PAYSTATUS.FAILED.getId(), USERAPPROVE);
                    res.setCode(20);
                } else {
                    res.setData(jsonObj.getString("message"));
                    RechargePaygateDaoImpl rechargePaygateDao = new RechargePaygateDaoImpl();
                    rechargePaygateDao.UpdateStatus(orderId, PayCommon.PAYSTATUS.FAILED.getId(), USERAPPROVE);
                    res.setCode(99);
                }
            } else {
                res.setCode(0);
                res.setData(jsonObj.getString("url"));
            }
        }
        catch (Exception e) {
            res.setCode(0);
            res.setData(result);
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
            logger.debug(("paywell Response: " + result.toString()));
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
            String desc = "ONLINE".equals(depositPayWellModel.getPayTypeStr()) ? "N\u1ea1p ti\u1ec1n nhanh Paywell" : "N\u1ea1p ti\u1ec1n Paywell";
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
            MoneyLogger.log(depositPayWellModel.Nickname, PAYMENTNAME, depositPayWellModel.Amount, 0L, "vin", "Nap vin qua paywell", "1031", "rmq error: " + e2.getMessage());
        }
        finally {
            userMap.unlock(depositPayWellModel.Nickname);
        }
        return res;
    }

    @Override
    public RechargePaywellResponse createTransaction(String userId, String username, String nickname, String fullName, long amount, String bankCode, String payType) {
        RechargePaywellResponse res = new RechargePaywellResponse(1, 0L, 0, 0L, "");
        try {
            res = this.createOrder(userId, username, nickname, amount, bankCode, payType);
            if (res.getCode() != 0) {
                return res;
            }
            long id = Long.parseLong(res.getTid());
            res = this.requestCreateTransaction(String.valueOf(id), amount, bankCode, payType, userId, fullName);
            return res;
        }
        catch (Exception e) {
            logger.debug(e);
            return res;
        }
    }

    @Override
    public RechargePaywellResponse callback(String cartId, String referenceId, long amount, long amountFee, Integer status, long requestTime, String signature) {
        RechargePaywellResponse res = new RechargePaywellResponse(1, 0L, 0, 0L, "");
        try {
            if (cartId.isEmpty() || referenceId.isEmpty() || amount < 0L || amountFee < 0L || signature.isEmpty()) {
                res.setData("PARRAM IS INVALID");
                return res;
            }
            Config config = this.getPaymentConfig().getConfig();
            RechargePaygateDaoImpl rechargeDao = new RechargePaygateDaoImpl();
            DepositPaygateModel model = rechargeDao.GetById(cartId);
            if (model == null) {
                return res;
            }
            String signature_string = "merchantCode=" + config.getMerchantCode() + "&cartId=" + cartId + "&referenceId=" + referenceId + "&amount=" + amount + "&amountFee=" + amountFee + "&currencyCode=" + config.getCurrencyCode() + "&status=" + status + "&requestTime=" + requestTime;
            String hash = PayCommon.getHMACSHA256(config.getMerchantKey(), signature_string);
            if (hash.isEmpty()) {
                res.setData("");
                return res;
            }
            if (!signature.equals(hash)) {
                res.setData("Invalid signature");
                return res;
            }
            if (!rechargeDao.UpdateStatus(cartId, referenceId, PayCommon.PAYSTATUS.RECEIVED.getId(), USERAPPROVE).booleanValue()) {
                res.setData("Update status not success");
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

    private BaseResponse<String> validateNotify(PaywellNotifyRequest requestObj) {
        String cartId = requestObj.getCartId();
        Double amount = requestObj.getAmount();
        Double amountFee = requestObj.getAmountFee();
        String referenceId = requestObj.getReferenceId();
        int status = requestObj.getStatus();
        long requestTime = requestObj.getRequestTime();
        String signature = requestObj.getSignature();
        String merchantCode = requestObj.getMerchantCode();
        String currencyCode = requestObj.getCurrencyCode();
        if (StringUtils.isBlank((CharSequence)cartId)) {
            return new BaseResponse<String>("5", "cartId is null or empty");
        }
        if (amount <= 0.0) {
            return new BaseResponse<String>("5", "amount < 0");
        }
        if (amountFee >= amount) {
            return new BaseResponse<String>("5", "amountFee >= amount");
        }
        if (StringUtils.isBlank((CharSequence)referenceId)) {
            return new BaseResponse<String>("5", "referenceId is null or empty");
        }
        if (StringUtils.isBlank((CharSequence)merchantCode)) {
            return new BaseResponse<String>("5", "merchantCode is null or empty");
        }
        if (StringUtils.isBlank((CharSequence)currencyCode)) {
            return new BaseResponse<String>("5", "currentcyCode is null or empty");
        }
        if (!RIGHT_STATUS.contains(status)) {
            return new BaseResponse<String>("5", "status wrong ,status=" + status);
        }
        if (requestTime <= 0L) {
            return new BaseResponse<String>("5", "requestTime is invalid");
        }
        if (StringUtils.isBlank((CharSequence)signature)) {
            return new BaseResponse<String>("5", "signature is null or empty");
        }
        return new BaseResponse<String>(true, "0", "SUCCESS", null);
    }

    @Override
    public RechargePaywellResponse notification(PaywellNotifyRequest requestObj) {
        RechargePaywellResponse res = new RechargePaywellResponse(1, 0L, 0, 0L, "");
        int paywellStatus = requestObj.getStatus();
        if (paywellStatus == 1) {
            res.setCode(0);
            res.setData("status =1 , ignore");
            return res;
        }
        BaseResponse<String> valid = this.validateNotify(requestObj);
        if ("0".equals(valid.getErrorCode())) {
            RechargePaygateDaoImpl rechargeDao = new RechargePaygateDaoImpl();
            try {
                DepositPaygateModel model = rechargeDao.GetById(requestObj.getCartId());
                if (model == null) {
                    return res;
                }
                if (model.Amount != requestObj.getAmount().longValue()) {
                    res.setData("Amount wrong");
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
                model.AmountFee = requestObj.getAmountFee().longValue();
                String paramArrays = "merchantCode=" + config.getMerchantCode() + "&cartId=" + requestObj.getCartId() + "&referenceId=" + requestObj.getReferenceId() + "&amount=" + model.Amount + "&amountFee=" + requestObj.getAmountFee().longValue() + "&currencyCode=" + requestObj.getCurrencyCode() + "&status=" + paywellStatus + "&requestTime=" + requestObj.getRequestTime();
                String signEncode = PayCommon.getHMACSHA256(this.getPaymentConfig().getConfig().getMerchantKey(), paramArrays);
                if (!requestObj.getSignature().equals(signEncode)) {
                    res.setData("Invalid signature");
                    return res;
                }
                if (paywellStatus == 3) {
                    boolean isUpdate = rechargeDao.UpdateStatus(requestObj.getCartId(), requestObj.getReferenceId(), PayCommon.PAYSTATUS.FAILED.getId(), USERAPPROVE);
                    if (!isUpdate) {
                        logger.error("paywell unable update status to fail");
                    }
                    res.setData("status updated to fail");
                    return res;
                }
                if (paywellStatus == 11) {
                    boolean isUpdate = rechargeDao.UpdateStatus(requestObj.getCartId(), requestObj.getReferenceId(), PayCommon.PAYSTATUS.SPAM.getId(), USERAPPROVE);
                    if (!isUpdate) {
                        logger.error("paywell unable update status to spam");
                    }
                    res.setData("status updated to spam");
                    return res;
                }
                if (paywellStatus == 2 || paywellStatus == 4) {
                    if (!rechargeDao.UpdateStatus(requestObj.getCartId(), requestObj.getReferenceId(), PayCommon.PAYSTATUS.SUCCESS.getId(), USERAPPROVE).booleanValue()) {
                        res.setData("UpdateStatus SUCCESS fail");
                        return res;
                    }
                    res = this.addMoney(model);
                    if (res.getCode() != 0) {
                        res.setData("Add money fail");
                        return res;
                    }
                    if (!rechargeDao.UpdateStatus(requestObj.getCartId(), requestObj.getReferenceId(), PayCommon.PAYSTATUS.COMPLETED.getId(), USERAPPROVE).booleanValue()) {
                        res.setData("UpdateStatus COMPLETED fail");
                        return res;
                    }
                    res.setCode(0);
                    return res;
                }
                res.setCode(0);
                res.setData("status =" + paywellStatus);
                return res;
            }
            catch (Exception e) {
                logger.debug(e);
                return res;
            }
        }
        res.setCode(Integer.parseInt(valid.getErrorCode()));
        res.setData(valid.getMessage());
        return res;
    }
}

