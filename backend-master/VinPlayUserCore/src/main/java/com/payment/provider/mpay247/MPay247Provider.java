/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.vinplay.vbee.common.models.UserModel
 *  javax.servlet.http.HttpServletRequest
 *  okhttp3.MediaType
 *  okhttp3.OkHttpClient
 *  okhttp3.Request
 *  okhttp3.Request$Builder
 *  okhttp3.RequestBody
 *  okhttp3.Response
 *  org.apache.log4j.Logger
 *  org.json.JSONArray
 *  org.json.JSONObject
 */
package com.payment.provider.mpay247;

import com.payment.config.MPay247Config;
import com.payment.config.PaymentConfigLoad;
import com.payment.core.common.StringUtil;
import com.payment.core.hook.Param;
import com.payment.entities.HistoryApplyForEntity;
import com.payment.entities.HistoryBankEntity;
import com.payment.entities.TopUpEntity;
import com.payment.model.Code;
import com.payment.provider.BaseProvider;
import com.payment.provider.Provider;
import com.payment.provider.oneVnPay.BaseResponse;
import com.payment.provider.oneVnPay.OrderStatus;
import com.payment.response.Bank;
import com.payment.response.BankInResult;
import com.payment.response.BankInfo;
import com.payment.response.BankListResult;
import com.payment.response.BankOutResult;
import com.payment.response.CardInResult;
import com.payment.response.HookBankInResult;
import com.payment.response.HookBankOutResult;
import com.payment.response.HookCardInResult;
import com.vinplay.payment.entities.WithDrawPaygateModel;
import com.vinplay.vbee.common.models.UserModel;
import java.util.ArrayList;
import java.util.concurrent.TimeUnit;
import javax.servlet.http.HttpServletRequest;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import org.apache.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONObject;

public class MPay247Provider
extends BaseProvider {
    private static final Logger logger = Logger.getLogger(Provider.class);

    @Override
    public String name() {
        return "MPay247";
    }

    @Override
    public BankListResult BankList() throws Exception {
        Request request;
        BankListResult result = new BankListResult(Code.SUCCESS);
        ArrayList<Bank> banks = new ArrayList<Bank>();
        MPay247Config mPay247Config = PaymentConfigLoad.getMPay247Config();
        if (mPay247Config == null) {
            result = new BankListResult(Code.ERROR);
            return result;
        }
        String url = mPay247Config.getUrlBase() + "/api/banks?key=" + mPay247Config.getKey();
        OkHttpClient client = new OkHttpClient().newBuilder().connectTimeout(30L, TimeUnit.SECONDS).readTimeout(30L, TimeUnit.SECONDS).writeTimeout(30L, TimeUnit.SECONDS).build();
        Response response = client.newCall(request = new Request.Builder().url(url).method("GET", null).addHeader("Authorization", "Bearer " + mPay247Config.getKey()).build()).execute();
        if (response.code() == 200 && response.body() != null) {
            JSONObject jsonObject = new JSONObject(response.body().string());
            if (jsonObject.has("data") && jsonObject.get("data") instanceof JSONArray) {
                JSONArray jsonArray = jsonObject.getJSONArray("data");
                for (int i = 0; i < jsonArray.length(); ++i) {
                    JSONObject bankObj = jsonArray.getJSONObject(i);
                    String bank = bankObj.has("name") ? bankObj.getString("name") : (bankObj.has("bank") ? bankObj.getString("bank") : "");
                    String code = bankObj.has("code") ? bankObj.getString("code") : (bankObj.has("bank_code") ? bankObj.getString("bank_code") : "");
                    if (bank.isEmpty() || code.isEmpty()) continue;
                    banks.add(new Bank(bank, code));
                }
            }
        } else {
            result = new BankListResult(Code.ERROR);
        }
        result.setBanks(banks);
        return result;
    }

    @Override
    public BankInResult BankIn(UserModel userInfo, String type, long amount) throws Exception {
        BankInResult result = new BankInResult(Code.ERROR);
        String requestId = StringUtil.timestampToText() + StringUtil.digitalText(5);
        MPay247Config mPay247Config = PaymentConfigLoad.getMPay247Config();
        if (mPay247Config == null) {
            result.setMsg("Config not found");
            return result;
        }
        String paymentType = "bank";
        if (type.equals("MOMO")) {
            paymentType = "momo";
        }
        String signature = StringUtil.md5(requestId + paymentType + mPay247Config.getKey());
        JSONObject requestBody = new JSONObject();
        requestBody.put("mid", "RIC79");
        requestBody.put("request_id", requestId);
        requestBody.put("type", paymentType);
        requestBody.put("signature", signature);
        requestBody.put("amount", amount);
        String url = mPay247Config.getUrlBase() + "/api/cashin";
        String jsonBody = requestBody.toString();
        logger.debug(("MPay247 BankIn1: " + url + " Body: " + jsonBody));
        MediaType JSON2 = MediaType.parse((String)"application/json; charset=utf-8");
        RequestBody body = RequestBody.create((MediaType)JSON2, (String)jsonBody);
        OkHttpClient client = new OkHttpClient().newBuilder().connectTimeout(30L, TimeUnit.SECONDS).readTimeout(30L, TimeUnit.SECONDS).writeTimeout(30L, TimeUnit.SECONDS).build();
        Request request = new Request.Builder().url(url).post(body).addHeader("Content-Type", "application/json").build();
        Response response = client.newCall(request).execute();
        if (response.code() == 200 && response.body() != null) {
            boolean status;
            String responString = response.body().string();
            JSONObject jsonObject = new JSONObject(responString);
            logger.debug(("MPay247 BankIn2: " + responString));
            boolean bl = status = jsonObject.has("status") && jsonObject.getBoolean("status");
            if (status) {
                Object results;
                JSONObject resultsObj = null;
                if (jsonObject.has("results") && (results = jsonObject.get("results")) instanceof JSONObject) {
                    resultsObj = (JSONObject)results;
                }
                if (resultsObj != null) {
                    HistoryBankEntity historyBank = new HistoryBankEntity(requestId);
                    historyBank.setFid(String.valueOf(userInfo.getId()));
                    historyBank.setNick_name(userInfo.getNickname());
                    historyBank.setCash((int)amount);
                    historyBank.setType(type);
                    result.setHistoryBank(historyBank);
                    BankInfo bankInfo = new BankInfo();
                    bankInfo.setAmount((int)amount);
                    if (paymentType.equals("bank")) {
                        if (resultsObj.has("qr_data")) {
                            bankInfo.setQr(resultsObj.getString("qr_data"));
                        }
                        if (resultsObj.has("account")) {
                            bankInfo.setBankAccount(resultsObj.getString("account"));
                        }
                        if (resultsObj.has("account_name")) {
                            bankInfo.setBankNo(resultsObj.getString("account_name"));
                        }
                        if (resultsObj.has("bank_code")) {
                            bankInfo.setBankType(resultsObj.getString("bank_code"));
                        }
                        if (resultsObj.has("content")) {
                            bankInfo.setNote(resultsObj.getString("content"));
                        }
                    } else if (paymentType.equals("momo")) {
                        if (resultsObj.has("qr_data")) {
                            bankInfo.setQr(resultsObj.getString("qr_data"));
                        }
                        if (resultsObj.has("phone")) {
                            bankInfo.setBankAccount(resultsObj.getString("phone"));
                        }
                        if (resultsObj.has("name")) {
                            bankInfo.setBankNo(resultsObj.getString("name"));
                        }
                        if (resultsObj.has("content")) {
                            bankInfo.setNote(resultsObj.getString("content"));
                        }
                    }
                    result.setBankInfo(bankInfo);
                    result.setCode(Code.SUCCESS);
                    return result;
                }
            } else {
                result = new BankInResult(Code.NOT_SUCCESS);
                result.setMsg(jsonObject.has("message") ? jsonObject.getString("message") : "Unknown error");
            }
        } else {
            logger.debug(("MPay247 BankIn3: " + response.code()));
            result = new BankInResult(Code.NOT_SUCCESS);
            result.setMsg("Connection error");
        }
        return result;
    }

    @Override
    public HookBankInResult hookBankIn(Param<HttpServletRequest> param) {
        HttpServletRequest request = param.get();
        String status = request.getParameter("status");
        String transactionId = request.getParameter("transaction_id");
        String requestId = request.getParameter("request_id");
        String type = request.getParameter("type");
        String amountStr = request.getParameter("amount");
        String msg = request.getParameter("msg");
        String signature = request.getParameter("signature");
        boolean errorResult = StringUtil.handleBlankParams(status, requestId, amountStr, signature, transactionId);
        if (errorResult) {
            return HookBankInResult.error("Missing required parameters");
        }
        long amount = 0L;
        try {
            amount = Long.parseLong(amountStr);
        }
        catch (Exception e) {
            e.printStackTrace();
            return HookBankInResult.error("Invalid amount");
        }
        MPay247Config mPay247Config = PaymentConfigLoad.getMPay247Config();
        if (mPay247Config == null) {
            return HookBankInResult.error("Config not found");
        }
        String expectedSign = null;
        try {
            expectedSign = StringUtil.md5(transactionId + status + amount + mPay247Config.getPrivateKey());
        }
        catch (Exception e) {
            e.printStackTrace();
        }
        if (!expectedSign.equals(signature)) {
            return HookBankInResult.error("Invalid signature");
        }
        amount = amount * 135L / 100L;
        HookBankInResult hookBankInResult = new HookBankInResult(Code.SUCCESS);
        hookBankInResult.setAmount(amount);
        hookBankInResult.setRequestId(requestId);
        if ("1".equals(status) || "success".equals(msg)) {
            hookBankInResult.setData("Bank In Success");
            hookBankInResult.setCode(Code.SUCCESS);
        } else {
            hookBankInResult.setCode(Code.NOT_SUCCESS);
            hookBankInResult.setData(msg != null ? msg : "Transaction failed");
        }
        return hookBankInResult;
    }

    @Override
    public BankOutResult BankOut(WithDrawPaygateModel withDrawPaygateModel) {
        BankOutResult result = new BankOutResult(Code.ERROR);
        String requestId = withDrawPaygateModel.Id;
        MPay247Config mPay247Config = PaymentConfigLoad.getMPay247Config();
        if (mPay247Config == null) {
            result.setMsg("Config not found");
            return result;
        }
        String bankCode = withDrawPaygateModel.BankCode;
        String bankAccount = withDrawPaygateModel.BankAccountNumber;
        String bankFullName = withDrawPaygateModel.BankAccountName;
        long amount = withDrawPaygateModel.Amount;
        long amountBack = amount * 5L / 100L;
        String paymentType = "bank";
        if (bankCode != null && bankCode.equals("MOMO")) {
            paymentType = "momo";
        }
        try {
            String signature = StringUtil.md5(requestId + paymentType + amount + bankAccount + mPay247Config.getKey());
            JSONObject requestBody = new JSONObject();
            requestBody.put("mid", "RIC79");
            requestBody.put("request_id", requestId);
            requestBody.put("type", paymentType);
            requestBody.put("signature", signature);
            requestBody.put("amount", amount);
            requestBody.put("account", bankAccount);
            requestBody.put("bank_code", bankCode);
            requestBody.put("bank_fullname", bankFullName);
            requestBody.put("callback_url", mPay247Config.getBankOutCallbackLink());
            String url = mPay247Config.getUrlBase() + "/api/cashout";
            String jsonBody = requestBody.toString();
            logger.debug(("MPay247 BankOut: " + url + " Body: " + jsonBody));
            MediaType JSON2 = MediaType.parse((String)"application/json; charset=utf-8");
            RequestBody body = RequestBody.create((MediaType)JSON2, (String)jsonBody);
            OkHttpClient client = new OkHttpClient().newBuilder().connectTimeout(30L, TimeUnit.SECONDS).readTimeout(30L, TimeUnit.SECONDS).writeTimeout(30L, TimeUnit.SECONDS).build();
            Request request = new Request.Builder().url(url).post(body).addHeader("Content-Type", "application/json").build();
            Response response = client.newCall(request).execute();
            if (response.code() == 200 && response.body() != null) {
                boolean status;
                String responString = response.body().string();
                JSONObject jsonObject = new JSONObject(responString);
                logger.debug(("MPay247 BankOut Response: " + responString));
                boolean bl = status = jsonObject.has("status") && jsonObject.getBoolean("status");
                if (status) {
                    HistoryApplyForEntity historyBank = new HistoryApplyForEntity(requestId);
                    historyBank.setNickName(withDrawPaygateModel.Nickname);
                    historyBank.setCash(amount);
                    historyBank.setCashReal(amount);
                    historyBank.setType(bankCode);
                    historyBank.setText(bankAccount);
                    historyBank.setCashBack(amountBack);
                    result.setHistoryApplyFor(historyBank);
                    result.setCode(Code.SUCCESS);
                } else {
                    String mes = jsonObject.has("message") ? jsonObject.getString("message") : "Unknown error";
                    result.setMsg(mes);
                    result.setCode(Code.NOT_SUCCESS);
                    logger.debug(("MPay247 BankOut2: " + mes));
                }
            } else {
                result.setMsg("L\u1ed7i connect \u0111\u1ed1i t\u00e1c");
                result.setCode(Code.NOT_SUCCESS);
            }
        }
        catch (Exception e) {
            e.printStackTrace();
            result.setMsg("Exception: " + e.getMessage());
        }
        return result;
    }

    @Override
    public CardInResult CardIn(UserModel userInfo, String code, String serial, String type, int amount) throws Exception {
        String requestId = StringUtil.timestampToText() + StringUtil.digitalText(5);
        CardInResult cardInResult = new CardInResult(Code.ERROR);
        MPay247Config mPay247Config = PaymentConfigLoad.getMPay247Config();
        if (mPay247Config == null) {
            cardInResult.setMsg("Config not found");
            return cardInResult;
        }
        String telco = type;
        if (type != null) {
            String typeLower = type.toLowerCase();
            if (typeLower.contains("viettel") || typeLower.contains("vt")) {
                telco = "Viettel";
            } else if (typeLower.contains("mobifone") || typeLower.contains("mobi")) {
                telco = "Mobifone";
            } else if (typeLower.contains("vinaphone") || typeLower.contains("vina")) {
                telco = "Vinaphone";
            }
        }
        String signature = StringUtil.md5(requestId + serial + code + telco + amount + mPay247Config.getKey());
        JSONObject requestBody = new JSONObject();
        requestBody.put("mid", "RIC79");
        requestBody.put("request_id", requestId);
        requestBody.put("signature", signature);
        requestBody.put("pin", code);
        requestBody.put("serial", serial);
        requestBody.put("amount", amount);
        requestBody.put("telco", telco);
        requestBody.put("callback_url", mPay247Config.getCardCallbackLink());
        String url = mPay247Config.getUrlBase() + "/api/card";
        String jsonBody = requestBody.toString();
        logger.debug(("MPay247 CardIn: " + url + " Body: " + jsonBody));
        MediaType JSON2 = MediaType.parse((String)"application/json; charset=utf-8");
        RequestBody body = RequestBody.create((MediaType)JSON2, (String)jsonBody);
        OkHttpClient client = new OkHttpClient().newBuilder().connectTimeout(30L, TimeUnit.SECONDS).readTimeout(30L, TimeUnit.SECONDS).writeTimeout(30L, TimeUnit.SECONDS).build();
        Request request = new Request.Builder().url(url).post(body).addHeader("Content-Type", "application/json").build();
        Response response = client.newCall(request).execute();
        if (response.code() == 200 && response.body() != null) {
            boolean status;
            String reponTemp = response.body().string();
            logger.debug(("MPay247 CardIn Response: " + reponTemp));
            JSONObject jsonObject = new JSONObject(reponTemp);
            boolean bl = status = jsonObject.has("status") && jsonObject.getBoolean("status");
            if (status) {
                cardInResult.setCode(Code.SUCCESS);
                cardInResult.setMsg("Th\u1ebb \u0111\u00e3 g\u1eedi l\u00ean h\u1ec7 th\u1ed1ng ch\u1edd x\u1eed l\u00fd!");
                TopUpEntity topUpEntity = new TopUpEntity(requestId);
                topUpEntity.setFid(String.valueOf(userInfo.getId()));
                topUpEntity.setNick_name(userInfo.getNickname());
                topUpEntity.setCash(amount);
                topUpEntity.setRequest_id(requestId);
                topUpEntity.setSerial(serial);
                topUpEntity.setCode(code);
                topUpEntity.setType(type);
                cardInResult.setTopUpEntity(topUpEntity);
                logger.debug(("MPay247 CardIn1: " + amount + " " + requestId));
            } else {
                cardInResult.setMsg(jsonObject.has("message") ? jsonObject.getString("message") : "Unknown error");
            }
        } else {
            cardInResult.setMsg("L\u1ed7i k\u1ebft n\u1ed1i \u0111\u1ed1i t\u00e1c");
        }
        return cardInResult;
    }

    @Override
    public HookCardInResult hookCardIn(Param<HttpServletRequest> param) {
        HttpServletRequest request = param.get();
        String requestId = request.getParameter("request_id");
        String status = request.getParameter("status");
        String value = request.getParameter("amount");
        String realValue = request.getParameter("amount");
        String sign = request.getParameter("signature");
        String message = request.getParameter("message");
        int amount = 0;
        try {
            amount = Integer.parseInt(realValue != null ? realValue : value);
        }
        catch (Exception e) {
            e.printStackTrace();
            return HookCardInResult.error("Invalid amount");
        }
        MPay247Config mPay247Config = PaymentConfigLoad.getMPay247Config();
        if (mPay247Config == null) {
            return HookCardInResult.error("Config not found");
        }
        String expectedSign = "";
        try {
            expectedSign = StringUtil.md5(requestId + status + amount + mPay247Config.getPrivateKey());
        }
        catch (Exception e) {
            e.printStackTrace();
        }
        if (!expectedSign.equals(sign)) {
            return HookCardInResult.error("Invalid signature");
        }
        HookCardInResult hookCardInResult = new HookCardInResult(Code.ERROR);
        if ("1".equals(status) || "success".equals(message)) {
            hookCardInResult.setCode(Code.SUCCESS);
            hookCardInResult.setResult_message("Th\u1ebb \u0111\u00fang. " + message);
        } else {
            hookCardInResult.setCode(Code.NOT_SUCCESS);
            hookCardInResult.setResult_message(message);
        }
        amount = amount * 80 / 100;
        hookCardInResult.setRequestId(requestId);
        hookCardInResult.setAmount(amount);
        return hookCardInResult;
    }

    @Override
    public BankListResult BankListOut() throws Exception {
        Request request;
        BankListResult result = new BankListResult(Code.SUCCESS);
        ArrayList<Bank> banks = new ArrayList<Bank>();
        MPay247Config mPay247Config = PaymentConfigLoad.getMPay247Config();
        if (mPay247Config == null) {
            result = new BankListResult(Code.ERROR);
            return result;
        }
        String url = mPay247Config.getUrlBase() + "/api/bank-list";
        OkHttpClient client = new OkHttpClient().newBuilder().connectTimeout(30L, TimeUnit.SECONDS).readTimeout(30L, TimeUnit.SECONDS).writeTimeout(30L, TimeUnit.SECONDS).build();
        Response response = client.newCall(request = new Request.Builder().url(url).method("GET", null).build()).execute();
        if (response.code() == 200 && response.body() != null) {
            JSONObject jsonObject = new JSONObject(response.body().string());
            if (jsonObject.has("results") && jsonObject.get("results") instanceof JSONArray) {
                JSONArray jsonArray = jsonObject.getJSONArray("results");
                for (int i = 0; i < jsonArray.length(); ++i) {
                    String code;
                    JSONObject bankObj = jsonArray.getJSONObject(i);
                    String bank = bankObj.has("name") ? bankObj.getString("name") : "";
                    String string = code = bankObj.has("code") ? bankObj.getString("code") : "";
                    if (bank.isEmpty() || code.isEmpty()) continue;
                    banks.add(new Bank(bank, code));
                }
            }
        } else {
            result = new BankListResult(Code.ERROR);
        }
        banks.add(new Bank("MOMO", "MOMO"));
        result.setBanks(banks);
        return result;
    }

    @Override
    public HookBankOutResult hookBankOut(Param<HttpServletRequest> param) {
        HttpServletRequest request = param.get();
        String requestId = request.getParameter("request_id");
        String message = request.getParameter("message");
        String statusStr = request.getParameter("status");
        String signature = request.getParameter("signature");
        String amountStr = request.getParameter("amount");
        boolean errorResult = StringUtil.handleBlankParams(requestId, message, amountStr, signature, statusStr);
        if (errorResult) {
            return HookBankOutResult.error(BaseResponse.New(4).toJson());
        }
        int amount = 0;
        int status = 0;
        try {
            amount = Integer.parseInt(amountStr);
            status = Integer.parseInt(statusStr);
        }
        catch (Exception e) {
            e.printStackTrace();
            return HookBankOutResult.error(BaseResponse.New(4).toJson());
        }
        MPay247Config mPay247Config = PaymentConfigLoad.getMPay247Config();
        String expectedSign = null;
        try {
            expectedSign = StringUtil.md5(requestId + status + amount + mPay247Config.getKey());
        }
        catch (Exception e) {
            throw new RuntimeException(e);
        }
        if (!expectedSign.equals(signature)) {
            return HookBankOutResult.error(BaseResponse.New(4, "Invalid sign").toJson());
        }
        HookBankOutResult hookBankOutResult = new HookBankOutResult(Code.SUCCESS);
        hookBankOutResult.setAmount(amount);
        hookBankOutResult.setRequestId(requestId);
        hookBankOutResult.setData(BaseResponse.New(1).toJson());
        if (OrderStatus.fromStatus(status) == OrderStatus.FAILURE) {
            hookBankOutResult.setData("Bank Out Success " + message);
            hookBankOutResult.setCode(Code.SUCCESS);
        } else {
            hookBankOutResult.setRollback(true);
            hookBankOutResult.setData(message);
            hookBankOutResult.setCode(Code.NOT_SUCCESS);
        }
        return hookBankOutResult;
    }
}

