/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.gson.JsonObject
 *  com.vinplay.vbee.common.models.UserModel
 *  javax.servlet.http.HttpServletRequest
 *  org.apache.http.client.utils.URIBuilder
 *  org.apache.log4j.Logger
 *  org.json.JSONObject
 *  org.jsoup.Jsoup
 *  org.jsoup.nodes.Document
 *  org.jsoup.nodes.Element
 */
package com.payment.provider.oneVnPay;

import com.google.gson.JsonObject;
import com.payment.config.OneVnPayConfig;
import com.payment.config.PaymentConfigLoad;
import com.payment.core.common.HttpUtils;
import com.payment.core.common.StringUtil;
import com.payment.core.hook.Param;
import com.payment.entities.HistoryApplyForEntity;
import com.payment.entities.HistoryBankEntity;
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
import com.payment.response.HookBankInResult;
import com.payment.response.HookBankOutResult;
import com.vinplay.payment.entities.WithDrawPaygateModel;
import com.vinplay.vbee.common.models.UserModel;
import java.util.ArrayList;
import java.util.Objects;
import javax.servlet.http.HttpServletRequest;
import org.apache.http.client.utils.URIBuilder;
import org.apache.log4j.Logger;
import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

public class OneVnPayProvider
extends BaseProvider {
    private static final Logger logger = Logger.getLogger(Provider.class);

    @Override
    public String name() {
        return "OneVnPay";
    }

    @Override
    public BankInResult BankIn(UserModel userInfo, String type, long amount) throws Exception {
        BankInResult result = new BankInResult(Code.ERROR);
        String requestId = StringUtil.timestampToText() + StringUtil.digitalText(5);
        OneVnPayConfig vnPayConfig = PaymentConfigLoad.getOneVnPayConfig();
        String channel = "bank_transfer";
        String bankCode = type;
        if (type.equals("MOMO")) {
            channel = "momo_qr";
            bankCode = "momo";
        }
        String signature = StringUtil.md5(vnPayConfig.getMerchantNo() + "|" + requestId + "|" + amount + "|" + channel + "|" + vnPayConfig.getKey());
        try {
            URIBuilder urlBuild = new URIBuilder(vnPayConfig.getUrlBase());
            urlBuild.setPath("/api/v1/createOrder");
            JsonObject jsonInput = new JsonObject();
            jsonInput.addProperty("amount", (Number)amount);
            jsonInput.addProperty("channel", channel);
            jsonInput.addProperty("bank_code", bankCode);
            jsonInput.addProperty("notify_url", vnPayConfig.getBankCallbackLink());
            jsonInput.addProperty("merchant_no", vnPayConfig.getMerchantNo());
            jsonInput.addProperty("sign", signature);
            jsonInput.addProperty("order_no", requestId);
            String jsonInputString = jsonInput.toString();
            System.out.println("Create Order To -> " + urlBuild.build().toString() + " => body: " + jsonInputString);
            String data = HttpUtils.postData(urlBuild.build().toString(), jsonInputString);
            System.out.println("Result Order To Response: " + data);
            JSONObject obj = new JSONObject(data);
            if (obj.has("code") && obj.getInt("code") == 0) {
                HistoryBankEntity historyBank = new HistoryBankEntity(requestId);
                historyBank.setFid(String.valueOf(userInfo.getId()));
                historyBank.setNick_name(userInfo.getNickname());
                historyBank.setCash((int)amount);
                historyBank.setType(type);
                result.setHistoryBank(historyBank);
                BankInfo bankInfo = this.getBankInfo(obj);
                bankInfo.setAmount((int)amount);
                result.setBankInfo(bankInfo);
                result.setCode(Code.SUCCESS);
                return result;
            }
            result.setMsg(obj.getString("message"));
            result.setCode(Code.NOT_SUCCESS);
        }
        catch (Exception e) {
            e.printStackTrace();
            logger.debug(e);
            result.setMsg(e.toString());
        }
        return result;
    }

    @Override
    public BankOutResult BankOut(WithDrawPaygateModel withDrawPaygateModel) throws Exception {
        BankOutResult result = new BankOutResult(Code.ERROR);
        String requestId = withDrawPaygateModel.Id;
        OneVnPayConfig vnPayConfig = PaymentConfigLoad.getOneVnPayConfig();
        String channel = "bank";
        String bankCode = withDrawPaygateModel.BankCode;
        String bankAccount = withDrawPaygateModel.BankAccountNumber;
        String bankFullName = withDrawPaygateModel.BankAccountName;
        long amount = withDrawPaygateModel.Amount;
        String signature = StringUtil.md5(vnPayConfig.getMerchantNo() + "|" + requestId + "|" + amount + "|" + channel + "|" + vnPayConfig.getKey());
        try {
            URIBuilder urlBuild = new URIBuilder(vnPayConfig.getUrlBase());
            urlBuild.setPath("/api/v2/payOut");
            JsonObject jsonInput = new JsonObject();
            jsonInput.addProperty("amount", (Number)amount);
            jsonInput.addProperty("channel", channel);
            jsonInput.addProperty("bank_code", bankCode);
            jsonInput.addProperty("notify_url", vnPayConfig.getBankOutCallbackLink());
            jsonInput.addProperty("merchant_no", vnPayConfig.getMerchantNo());
            jsonInput.addProperty("bank_number", bankAccount);
            jsonInput.addProperty("beneficiary_name", bankFullName);
            jsonInput.addProperty("sign", signature);
            jsonInput.addProperty("order_no", requestId);
            String jsonInputString = jsonInput.toString();
            System.out.println("Create Out -> " + urlBuild.build().toString() + " => body: " + jsonInputString);
            String data = HttpUtils.postData(urlBuild.build().toString(), jsonInputString);
            System.out.println("Result Out Response: " + data);
            JSONObject obj = new JSONObject(data);
            if (obj.has("status") && obj.getInt("status") == 0) {
                HistoryApplyForEntity historyBank = new HistoryApplyForEntity(requestId);
                historyBank.setNickName(withDrawPaygateModel.Nickname);
                historyBank.setCash(amount);
                historyBank.setCashReal(amount);
                historyBank.setType(bankCode);
                historyBank.setText(bankAccount);
                result.setHistoryApplyFor(historyBank);
                result.setCode(Code.SUCCESS);
            } else {
                result.setMsg(obj.getString("message"));
                result.setCode(Code.NOT_SUCCESS);
            }
        }
        catch (Exception e) {
            e.printStackTrace();
            logger.debug(e);
            result.setMsg(e.toString());
        }
        return result;
    }

    @Override
    public BankListResult BankList() throws Exception {
        BankListResult bankListResult = new BankListResult(Code.SUCCESS);
        ArrayList<Bank> banks = new ArrayList<Bank>();
        banks.add(new Bank("VP BANK", "VP"));
        banks.add(new Bank("ACB BANK", "ACB"));
        banks.add(new Bank("BIDV BANK", "BIDV"));
        banks.add(new Bank("VIETTIN BANK", "VTB"));
        banks.add(new Bank("MB BANK", "MB"));
        banks.add(new Bank("EXIM BANK", "EXB"));
        banks.add(new Bank("SACOM", "SAC"));
        banks.add(new Bank("TECHCOM BANK", "TCB"));
        banks.add(new Bank("VIETCOM BANK", "VCB"));
        banks.add(new Bank("DONGA BANK", "DAB"));
        banks.add(new Bank("VIB BANK", "VIB"));
        banks.add(new Bank("MSB BANK", "MSB"));
        banks.add(new Bank("SHB BANK", "SHB"));
        bankListResult.setBanks(banks);
        return bankListResult;
    }

    @Override
    public HookBankInResult hookBankIn(Param<HttpServletRequest> param) {
        HttpServletRequest request = param.get();
        String resultCode = request.getParameter("result_code");
        String merchantNo = request.getParameter("merchant_no");
        String orderNo = request.getParameter("order_no");
        String yltOrderNo = request.getParameter("ylt_order_no");
        String amountStr = request.getParameter("amount");
        String channel = request.getParameter("channel");
        String extraParam = request.getParameter("extra_param");
        String sign = request.getParameter("sign");
        String userAmountStr = request.getParameter("user_amount");
        boolean errorResult = StringUtil.handleBlankParams(resultCode, merchantNo, orderNo, yltOrderNo, amountStr, channel);
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
        OneVnPayConfig vnPayConfig = PaymentConfigLoad.getOneVnPayConfig();
        String expectedSign = null;
        try {
            expectedSign = StringUtil.md5(merchantNo + "|" + orderNo + "|" + yltOrderNo + "|" + amount + "|" + channel + "|" + vnPayConfig.getKey());
        }
        catch (Exception e) {
            throw new RuntimeException(e);
        }
        if (!expectedSign.equals(sign)) {
            return HookBankInResult.error(BaseResponse.New(4, "Invalid sign").toJson());
        }
        HookBankInResult hookBankInResult = new HookBankInResult(Code.SUCCESS);
        hookBankInResult.setAmount(amount);
        hookBankInResult.setRequestId(orderNo);
        hookBankInResult.setData(BaseResponse.New(1).toJson());
        if ("success".equals(resultCode)) {
            hookBankInResult.setData("Bank In Success");
            hookBankInResult.setCode(Code.SUCCESS);
        } else {
            hookBankInResult.setCode(Code.NOT_SUCCESS);
        }
        return hookBankInResult;
    }

    @Override
    public HookBankOutResult hookBankOut(Param<HttpServletRequest> param) {
        HttpServletRequest request = param.get();
        String merchantNo = request.getParameter("merchant_no");
        String orderNo = request.getParameter("order_no");
        String amountStr = request.getParameter("amount");
        String yltOrderNo = request.getParameter("ylt_order_no");
        String statusStr = request.getParameter("status");
        String sign = request.getParameter("sign");
        boolean errorResult = StringUtil.handleBlankParams(merchantNo, orderNo, amountStr, yltOrderNo, statusStr, sign);
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
        OneVnPayConfig vnPayConfig = PaymentConfigLoad.getOneVnPayConfig();
        String expectedSign = null;
        try {
            expectedSign = StringUtil.md5(merchantNo + "|" + orderNo + "|" + yltOrderNo + "|" + amount + "|" + vnPayConfig.getKey());
        }
        catch (Exception e) {
            throw new RuntimeException(e);
        }
        if (!expectedSign.equals(sign)) {
            return HookBankOutResult.error(BaseResponse.New(4, "Invalid sign").toJson());
        }
        HookBankOutResult hookBankOutResult = new HookBankOutResult(Code.SUCCESS);
        hookBankOutResult.setAmount(amount);
        hookBankOutResult.setRequestId(orderNo);
        hookBankOutResult.setData(BaseResponse.New(1).toJson());
        if (OrderStatus.fromStatus(status) == OrderStatus.SUCCESS) {
            hookBankOutResult.setData("Bank Out Success");
            hookBankOutResult.setCode(Code.SUCCESS);
        } else {
            hookBankOutResult.setRollback(true);
            switch (Objects.requireNonNull(OrderStatus.fromStatus(status))) {
                case FAILURE: {
                    hookBankOutResult.setData("Bank Out Failure");
                    break;
                }
                case ABNORMAL_TRANSACTION: {
                    hookBankOutResult.setData("Bank Out Abnormal Transaction");
                    break;
                }
                case INSUFFICIENT_BALANCE: {
                    hookBankOutResult.setData("Bank Out Insufficient Balance");
                    break;
                }
                case REJECT: {
                    hookBankOutResult.setData("Bank Out Reject");
                    break;
                }
                default: {
                    hookBankOutResult.setData("Bank Out Unknown");
                }
            }
            hookBankOutResult.setCode(Code.NOT_SUCCESS);
        }
        return hookBankOutResult;
    }

    private BankInfo getBankInfo(JSONObject obj) {
        BankInfo bankInfo = new BankInfo();
        try {
            String url = obj.getString("data");
            Document doc = Jsoup.connect((String)url).get();
            Element qrCodeBox = doc.select(".qr-code-box img").first();
            String qrCodeSrc = qrCodeBox != null ? qrCodeBox.attr("src") : null;
            bankInfo.setQr(qrCodeSrc);
            Element bankName = doc.getElementById("bankName");
            String bankNameValue = bankName != null ? bankName.text() : null;
            bankInfo.setBankType(bankNameValue);
            Element accountNo = doc.getElementById("accountNo");
            String accountNoValue = accountNo != null ? accountNo.text() : null;
            bankInfo.setBankNo(accountNoValue);
            Element username = doc.getElementById("username");
            String usernameValue = username != null ? username.text() : null;
            bankInfo.setBankAccount(usernameValue);
            Element desc = doc.getElementById("desc");
            String descValue = desc != null ? desc.text() : null;
            bankInfo.setNote(descValue);
            bankInfo.setQrUrl(url);
        }
        catch (Exception e) {
            e.printStackTrace();
        }
        return bankInfo;
    }
}

