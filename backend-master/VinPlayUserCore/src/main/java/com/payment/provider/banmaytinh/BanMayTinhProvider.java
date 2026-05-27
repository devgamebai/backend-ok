/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.vinplay.vbee.common.models.UserModel
 *  javax.servlet.http.HttpServletRequest
 *  okhttp3.OkHttpClient
 *  okhttp3.Request
 *  okhttp3.Request$Builder
 *  okhttp3.Response
 *  org.apache.log4j.Logger
 *  org.json.JSONArray
 *  org.json.JSONObject
 */
package com.payment.provider.banmaytinh;

import com.payment.config.BanMayTinhConfig;
import com.payment.config.PaymentConfigLoad;
import com.payment.core.common.StringUtil;
import com.payment.core.hook.Param;
import com.payment.entities.HistoryApplyForEntity;
import com.payment.entities.HistoryBankEntity;
import com.payment.entities.TopUpEntity;
import com.payment.model.Code;
import com.payment.provider.BaseProvider;
import com.payment.provider.Provider;
import com.payment.provider.banmaytinh.BaseResponse;
import com.payment.response.Bank;
import com.payment.response.BankInResult;
import com.payment.response.BankInfo;
import com.payment.response.BankListResult;
import com.payment.response.BankOutResult;
import com.payment.response.CardInResult;
import com.payment.response.HookBankInResult;
import com.payment.response.HookCardInResult;
import com.vinplay.payment.entities.WithDrawPaygateModel;
import com.vinplay.vbee.common.models.UserModel;
import java.util.ArrayList;
import javax.servlet.http.HttpServletRequest;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.apache.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONObject;

public class BanMayTinhProvider
extends BaseProvider {
    private static final Logger logger = Logger.getLogger(Provider.class);

    @Override
    public String name() {
        return "BanMayTinh";
    }

    @Override
    public BankListResult BankList() throws Exception {
        Request request;
        BankListResult result = new BankListResult(Code.SUCCESS);
        ArrayList<Bank> banks = new ArrayList<Bank>();
        BanMayTinhConfig banMayTinhConfig = PaymentConfigLoad.getBanMayTinhConfig();
        String url = banMayTinhConfig.getUrlBase() + "api/listbank?key=" + banMayTinhConfig.getKey();
        OkHttpClient client = new OkHttpClient().newBuilder().build();
        Response response = client.newCall(request = new Request.Builder().url(url).method("GET", null).build()).execute();
        if (response.code() == 200 && response.body() != null) {
            JSONObject jsonObject = new JSONObject(response.body().string());
            JSONArray jsonArray = jsonObject.getJSONArray("data");
            for (int i = 0; i < jsonArray.length(); ++i) {
                String bank = jsonArray.getJSONObject(i).getString("bank");
                String code = jsonArray.getJSONObject(i).getString("bank_code");
                banks.add(new Bank(bank, code));
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
        BanMayTinhConfig banMayTinhConfig = PaymentConfigLoad.getBanMayTinhConfig();
        String cardType = "BANK";
        String bankCode = type;
        if (type.equals("MOMO")) {
            cardType = "MOMO";
            bankCode = "momo";
        }
        String signature = StringUtil.md5(banMayTinhConfig.getPrivateKey() + requestId);
        String url = banMayTinhConfig.getUrlBase() + "api/regcharge?key=" + banMayTinhConfig.getKey() + "&cardType=" + cardType + "&amount=" + amount + "&refcode=" + requestId + "&signature=" + signature + "&bankcode=" + bankCode + "&callbackUrl=" + banMayTinhConfig.getBankCallbackLink();
        logger.debug(("BankIn1: " + url));
        OkHttpClient client = new OkHttpClient().newBuilder().build();
        Request request = new Request.Builder().url(url).method("GET", null).build();
        Response response = client.newCall(request).execute();
        if (response.code() == 200 && response.body() != null) {
            String responString = response.body().string();
            JSONObject jsonObject = new JSONObject(responString);
            logger.debug(("BankIn2: " + responString));
            if (jsonObject.getInt("status") == 1) {
                JSONObject jsonObject1 = jsonObject.getJSONArray("data").getJSONObject(0);
                HistoryBankEntity historyBank = new HistoryBankEntity(requestId);
                historyBank.setFid(String.valueOf(userInfo.getId()));
                historyBank.setNick_name(userInfo.getNickname());
                historyBank.setCash((int)amount);
                historyBank.setType(type);
                result.setHistoryBank(historyBank);
                BankInfo bankInfo = new BankInfo();
                bankInfo.setAmount((int)amount);
                bankInfo.setQr(jsonObject1.getString("qrcode"));
                bankInfo.setBankAccount(jsonObject1.getString("bank_account"));
                bankInfo.setBankNo(jsonObject1.getString("bank_name"));
                bankInfo.setBankType(jsonObject1.getString("bank_code"));
                bankInfo.setNote(jsonObject1.getString("prefix"));
                result.setBankInfo(bankInfo);
                result.setCode(Code.SUCCESS);
                return result;
            }
            result = new BankInResult(Code.NOT_SUCCESS);
            result.setMsg(jsonObject.getString("msg"));
        } else {
            logger.debug(("BankIn3: " + response.code()));
            result = new BankInResult(Code.NOT_SUCCESS);
        }
        return result;
    }

    @Override
    public HookBankInResult hookBankIn(Param<HttpServletRequest> param) {
        HttpServletRequest request = param.get();
        String status = request.getParameter("status");
        String bank_id = request.getParameter("bank_id");
        String refcode = request.getParameter("refcode");
        String type = request.getParameter("type");
        String amountStr = request.getParameter("amount");
        String code = request.getParameter("code");
        String option = request.getParameter("option");
        String msg = request.getParameter("msg");
        String signature = request.getParameter("signature");
        boolean errorResult = StringUtil.handleBlankParams(status, bank_id, refcode, type, amountStr, code, option, signature);
        if (errorResult) {
            return HookBankInResult.error(BaseResponse.New(4).toJson());
        }
        int amount = 0;
        try {
            amount = Integer.parseInt(amountStr);
        }
        catch (Exception e) {
            e.printStackTrace();
            return HookBankInResult.error(BaseResponse.New(4).toJson());
        }
        BanMayTinhConfig banMayTinhConfig = PaymentConfigLoad.getBanMayTinhConfig();
        String expectedSign = null;
        try {
            expectedSign = StringUtil.md5(banMayTinhConfig.getPrivateKey() + bank_id);
        }
        catch (Exception e) {
            throw new RuntimeException(e);
        }
        if (!expectedSign.equals(signature)) {
            return HookBankInResult.error(com.payment.provider.oneVnPay.BaseResponse.New(4, "Invalid sign").toJson());
        }
        HookBankInResult hookBankInResult = new HookBankInResult(Code.SUCCESS);
        hookBankInResult.setAmount(amount);
        hookBankInResult.setRequestId(refcode);
        hookBankInResult.setData(BaseResponse.New(1).toJson());
        if ("1".equals(status)) {
            hookBankInResult.setData("Bank In Success");
            hookBankInResult.setCode(Code.SUCCESS);
        } else {
            hookBankInResult.setCode(Code.NOT_SUCCESS);
        }
        return hookBankInResult;
    }

    @Override
    public BankOutResult BankOut(WithDrawPaygateModel withDrawPaygateModel) {
        BankOutResult result = new BankOutResult(Code.ERROR);
        String requestId = withDrawPaygateModel.Id;
        BanMayTinhConfig banMayTinhConfig = PaymentConfigLoad.getBanMayTinhConfig();
        String channel = "bank";
        String bankCode = withDrawPaygateModel.BankCode;
        String bankAccount = withDrawPaygateModel.BankAccountNumber;
        String bankFullName = withDrawPaygateModel.BankAccountName;
        long amount = withDrawPaygateModel.Amount;
        if (bankCode.equals("MOMO")) {
            channel = "momo";
        }
        String signature = null;
        try {
            signature = StringUtil.md5(banMayTinhConfig.getPrivateKey() + requestId);
            String url = banMayTinhConfig.getUrlBase() + "api/withdraw?key=" + banMayTinhConfig.getKey() + "&type=" + channel + "&requestid=" + requestId + "&account=" + bankAccount + "&bank_code=" + bankCode + "&bank_name=" + bankFullName + "&money=" + amount + "&signature=" + signature + "&callbackUrl=" + banMayTinhConfig.getBankOutCallbackLink();
            logger.debug(("BankOut: " + url));
            OkHttpClient client = new OkHttpClient().newBuilder().build();
            Request request = new Request.Builder().url(url).method("GET", null).build();
            Response response = client.newCall(request).execute();
            if (response.code() == 200 && response.body() != null) {
                JSONObject jsonObject = new JSONObject(response.body().string());
                if (jsonObject.has("status") && jsonObject.get("status").equals("1")) {
                    HistoryApplyForEntity historyBank = new HistoryApplyForEntity(requestId);
                    historyBank.setNickName(withDrawPaygateModel.Nickname);
                    historyBank.setCash(amount);
                    historyBank.setCashReal(amount);
                    historyBank.setType(bankCode);
                    historyBank.setText(bankAccount);
                    result.setHistoryApplyFor(historyBank);
                    result.setCode(Code.SUCCESS);
                } else {
                    String mes = jsonObject.getString("msg");
                    result.setMsg(mes);
                    result.setCode(Code.NOT_SUCCESS);
                    logger.debug(("BankOut2: " + mes));
                }
            } else {
                result.setMsg("L\u1ed7i connect \u0111\u1ed1i t\u00e1c");
                result.setCode(Code.NOT_SUCCESS);
            }
        }
        catch (Exception e) {
            e.printStackTrace();
        }
        return result;
    }

    @Override
    public CardInResult CardIn(UserModel userInfo, String code, String serial, String type, int amount) throws Exception {
        Request request;
        String requestId = StringUtil.timestampToText() + StringUtil.digitalText(5);
        CardInResult cardInResult = new CardInResult(Code.ERROR);
        BanMayTinhConfig banMayTinhConfig = PaymentConfigLoad.getBanMayTinhConfig();
        String signature = StringUtil.md5(banMayTinhConfig.getKey() + code + serial);
        String url = banMayTinhConfig.getUrlBase() + "api/sendCard_v3?key=" + banMayTinhConfig.getKey() + "&refcode=" + requestId + "&signature=" + signature + "&callbackUrl=" + banMayTinhConfig.getCardCallbackLink() + "&cardType=" + type + "&cardSeri=" + serial + "&cardCode=" + code + "&cardValue=" + amount;
        OkHttpClient client = new OkHttpClient().newBuilder().build();
        Response response = client.newCall(request = new Request.Builder().url(url).method("GET", null).build()).execute();
        if (response.code() == 200 && response.body() != null) {
            String reponTemp = response.body().string();
            logger.debug(("CardIn: " + reponTemp));
            JSONObject jsonObject = new JSONObject(reponTemp);
            if (jsonObject.has("status") && jsonObject.getString("status").equals("1")) {
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
                logger.debug(("CardIn1: " + amount + " " + requestId));
            } else {
                cardInResult.setMsg(jsonObject.getString("msg"));
            }
        } else {
            cardInResult.setMsg("L\u1ed7i k\u1ebft n\u1ed1i \u0111\u1ed1i t\u00e1c");
        }
        return cardInResult;
    }

    @Override
    public HookCardInResult hookCardIn(Param<HttpServletRequest> param) {
        HttpServletRequest request = param.get();
        String transactionId = request.getParameter("transaction_id");
        String status = request.getParameter("status");
        String value = request.getParameter("value");
        String realValue = request.getParameter("real_value");
        String receivedValue = request.getParameter("received_value");
        String cardSeri = request.getParameter("card_seri");
        String cardCode = request.getParameter("card_code");
        String refCode = request.getParameter("refcode");
        String sign = request.getParameter("sign");
        int amount = 0;
        try {
            amount = Integer.parseInt(realValue);
        }
        catch (Exception e) {
            e.printStackTrace();
            return HookCardInResult.error(BaseResponse.New(4).toJson());
        }
        BanMayTinhConfig banMayTinhConfig = PaymentConfigLoad.getBanMayTinhConfig();
        String expectedSign = null;
        try {
            expectedSign = StringUtil.md5(banMayTinhConfig.getPrivateKey() + transactionId);
        }
        catch (Exception e) {
            throw new RuntimeException(e);
        }
        if (!expectedSign.equals(sign)) {
            return HookCardInResult.error(BaseResponse.New(4, "Invalid sign").toJson());
        }
        HookCardInResult hookCardInResult = new HookCardInResult(Code.ERROR);
        if ("1".equals(status)) {
            hookCardInResult.setCode(Code.SUCCESS);
            hookCardInResult.setResult_message("Th\u1ebb \u0111\u00fang.");
        } else {
            hookCardInResult.setCode(Code.NOT_SUCCESS);
            hookCardInResult.setResult_message("Tr\u1ea1ng th\u00e1i kh\u00f4ng x\u00e1c \u0111\u1ecbnh.");
        }
        hookCardInResult.setRequestId(refCode);
        hookCardInResult.setAmount(amount);
        return hookCardInResult;
    }

    @Override
    public BankListResult BankListOut() throws Exception {
        Request request;
        BankListResult result = new BankListResult(Code.SUCCESS);
        ArrayList<Bank> banks = new ArrayList<Bank>();
        BanMayTinhConfig banMayTinhConfig = PaymentConfigLoad.getBanMayTinhConfig();
        String url = banMayTinhConfig.getUrlBase() + "api/bankcode?key=" + banMayTinhConfig.getKey();
        OkHttpClient client = new OkHttpClient().newBuilder().build();
        Response response = client.newCall(request = new Request.Builder().url(url).method("GET", null).build()).execute();
        if (response.code() == 200 && response.body() != null) {
            JSONObject jsonObject = new JSONObject(response.body().string());
            JSONArray jsonArray = jsonObject.getJSONArray("data");
            for (int i = 0; i < jsonArray.length(); ++i) {
                String bank = jsonArray.getJSONObject(i).getString("name");
                String code = jsonArray.getJSONObject(i).getString("code");
                banks.add(new Bank(bank, code));
            }
        } else {
            result = new BankListResult(Code.ERROR);
        }
        result.setBanks(banks);
        return result;
    }
}

