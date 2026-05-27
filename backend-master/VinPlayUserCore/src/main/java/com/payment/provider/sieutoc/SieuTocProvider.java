/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.vinplay.vbee.common.models.UserModel
 *  javax.servlet.http.HttpServletRequest
 *  org.apache.http.client.methods.CloseableHttpResponse
 *  org.apache.http.client.methods.HttpGet
 *  org.apache.http.client.methods.HttpUriRequest
 *  org.apache.http.client.utils.URIBuilder
 *  org.apache.http.impl.client.CloseableHttpClient
 *  org.apache.http.impl.client.HttpClients
 *  org.apache.http.params.BasicHttpParams
 *  org.apache.http.params.HttpParams
 *  org.apache.log4j.Logger
 *  org.json.JSONObject
 */
package com.payment.provider.sieutoc;

import com.payment.config.SieuTocConfig;
import com.payment.core.common.ProviderUtil;
import com.payment.core.common.StringUtil;
import com.payment.core.hook.Param;
import com.payment.entities.HistoryApplyForEntity;
import com.payment.entities.HistoryBankEntity;
import com.payment.entities.TopUpEntity;
import com.payment.model.Code;
import com.payment.provider.BaseProvider;
import com.payment.provider.Provider;
import com.payment.provider.sieutoc.BaseResponse;
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
import javax.servlet.http.HttpServletRequest;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpParams;
import org.apache.log4j.Logger;
import org.json.JSONObject;

public class SieuTocProvider
extends BaseProvider {
    private static final Logger logger = Logger.getLogger(Provider.class);
    private SieuTocConfig sieuTocConfig;

    @Override
    public String name() {
        return "SieuToc";
    }

    @Override
    public BankListResult BankList() throws Exception {
        BankListResult result = new BankListResult(Code.SUCCESS);
        ArrayList banks = new ArrayList();
        return result;
    }

    @Override
    public BankInResult BankIn(UserModel userInfo, String type, long amount) throws Exception {
        BankInResult result = new BankInResult(Code.ERROR);
        String requestId = StringUtil.timestampToText() + StringUtil.digitalText(5);
        try {
            CloseableHttpClient httpclient = HttpClients.createDefault();
            URIBuilder url = new URIBuilder(this.sieuTocConfig.getBaseUrl());
            url.setPath("/api3/requestPayment");
            HttpGet request = new HttpGet(url.build());
            BasicHttpParams params = new BasicHttpParams();
            params.setParameter("api_key", this.sieuTocConfig.getApiKey());
            params.setParameter("request_id", requestId);
            params.setParameter("amount", amount);
            params.setParameter("url_callback", this.sieuTocConfig.getBankCallbackLink());
            if (type.trim().toUpperCase().contains("MOMO")) {
                params.setParameter("bank_code", type);
            }
            request.setParams((HttpParams)params);
            CloseableHttpResponse response = httpclient.execute((HttpUriRequest)request);
            JSONObject obj = ProviderUtil.getResponse(response);
            if (obj.has("status") && obj.getInt("status") == 1) {
                HistoryBankEntity historyBank = new HistoryBankEntity(requestId);
                historyBank.setFid(String.valueOf(userInfo.getId()));
                historyBank.setNick_name(userInfo.getNickname());
                historyBank.setCash((int)amount);
                historyBank.setType(type);
                result.setHistoryBank(historyBank);
                BankInfo bankInfo = new BankInfo();
                bankInfo.setBankAccount(obj.getString("bank_account"));
                bankInfo.setBankNo(obj.getString("bank_no"));
                bankInfo.setAmount(obj.getInt("amount"));
                bankInfo.setBankType(obj.getString("bank_type"));
                bankInfo.setQr(obj.getString("qr"));
                bankInfo.setQrUrl(obj.getString("qr_url"));
                bankInfo.setNote(obj.getString("note"));
                result.setCode(Code.SUCCESS);
            } else {
                result.setMsg(obj.getString("message"));
                result.setCode(Code.NOT_SUCCESS);
            }
            return result;
        }
        catch (Exception e) {
            e.printStackTrace();
            logger.debug(e);
            result.setMsg(e.toString());
            return result;
        }
    }

    @Override
    public CardInResult CardIn(UserModel userInfo, String code, String serial, String type, int amount) throws Exception {
        CardInResult result = new CardInResult(Code.ERROR);
        String requestId = StringUtil.timestampToText() + StringUtil.digitalText(5);
        String signature = StringUtil.md5(this.sieuTocConfig.getApiKey() + amount + code + serial);
        try {
            CloseableHttpClient httpclient = HttpClients.createDefault();
            URIBuilder url = new URIBuilder(this.sieuTocConfig.getBaseUrl());
            url.setPath("/api/cardcharging2");
            HttpGet request = new HttpGet(url.build());
            BasicHttpParams params = new BasicHttpParams();
            params.setParameter("api_key", this.sieuTocConfig.getApiKey());
            params.setParameter("request_id", requestId);
            params.setParameter("card_seri", serial);
            params.setParameter("card_code", code);
            params.setParameter("card_type", type);
            params.setParameter("card_amount", amount);
            params.setParameter("signature", signature);
            params.setParameter("url_callback", this.sieuTocConfig.getCardCallbackLink());
            request.setParams((HttpParams)params);
            CloseableHttpResponse response = httpclient.execute((HttpUriRequest)request);
            JSONObject obj = ProviderUtil.getResponse(response);
            if (obj.has("status") && obj.getInt("status") == 0) {
                TopUpEntity topUpEntity = new TopUpEntity(requestId);
                topUpEntity.setFid(String.valueOf(userInfo.getId()));
                topUpEntity.setNick_name(userInfo.getNickname());
                topUpEntity.setCash(amount);
                topUpEntity.setCode(code);
                topUpEntity.setSerial(serial);
                topUpEntity.setType(type);
                result.setTopUpEntity(topUpEntity);
                result.setCode(Code.SUCCESS);
            } else {
                result.setMsg(obj.getString("message"));
                result.setCode(Code.NOT_SUCCESS);
            }
            return result;
        }
        catch (Exception e) {
            e.printStackTrace();
            logger.debug(e);
            result.setMsg(e.toString());
            return result;
        }
    }

    @Override
    public BankOutResult BankOut(WithDrawPaygateModel withDrawPaygateModel) throws Exception {
        BankOutResult result = new BankOutResult(Code.ERROR);
        String requestId = StringUtil.timestampToText() + StringUtil.digitalText(5);
        String bank_code = withDrawPaygateModel.BankCode;
        String bank_account = withDrawPaygateModel.BankAccountNumber;
        String bank_full_name = withDrawPaygateModel.AgentBankAccountName;
        long amount = withDrawPaygateModel.Amount;
        String signature = StringUtil.md5(requestId + this.sieuTocConfig.getApiKey() + bank_account + amount + this.sieuTocConfig.getApiKey());
        try {
            CloseableHttpClient httpclient = HttpClients.createDefault();
            URIBuilder url = new URIBuilder(this.sieuTocConfig.getBaseUrl());
            url.setPath("/api3/outBank");
            HttpGet request = new HttpGet(url.build());
            BasicHttpParams params = new BasicHttpParams();
            params.setParameter("api_key", this.sieuTocConfig.getApiKey());
            params.setParameter("request_id", requestId);
            params.setParameter("bank_code", bank_code);
            params.setParameter("bank_account", bank_account);
            params.setParameter("bank_fullname", bank_full_name);
            params.setParameter("amount", amount);
            params.setParameter("signature", signature);
            params.setParameter("url_callback", this.sieuTocConfig.getBankOutCallbackLink());
            request.setParams((HttpParams)params);
            CloseableHttpResponse response = httpclient.execute((HttpUriRequest)request);
            JSONObject obj = ProviderUtil.getResponse(response);
            if (obj.has("status") && obj.getInt("status") == 0) {
                HistoryApplyForEntity historyBank = new HistoryApplyForEntity(requestId);
                historyBank.setNickName(withDrawPaygateModel.Nickname);
                historyBank.setCash(amount);
                historyBank.setCashReal(amount);
                historyBank.setType(bank_code);
                historyBank.setText(bank_account);
                result.setHistoryApplyFor(historyBank);
                result.setCode(Code.SUCCESS);
            } else {
                result.setMsg(obj.getString("message"));
                result.setCode(Code.NOT_SUCCESS);
            }
            return result;
        }
        catch (Exception e) {
            e.printStackTrace();
            logger.debug(e);
            result.setMsg(e.toString());
            return result;
        }
    }

    @Override
    public HookBankInResult hookBankIn(Param<HttpServletRequest> param) {
        HttpServletRequest request = param.get();
        String status = request.getParameter("status");
        String amountStr = request.getParameter("amount");
        String requestId = request.getParameter("request_id");
        String message = request.getParameter("message");
        int amount = 0;
        try {
            amount = Integer.parseInt(amountStr);
        }
        catch (Exception e) {
            e.printStackTrace();
            return HookBankInResult.error(BaseResponse.New(4).toJson());
        }
        boolean errorResult = StringUtil.handleBlankParams(status, amountStr, requestId, message);
        if (errorResult) {
            return HookBankInResult.error(BaseResponse.New(4).toJson());
        }
        HookBankInResult hookBankInResult = new HookBankInResult(Code.SUCCESS);
        hookBankInResult.setAmount(amount);
        hookBankInResult.setRequestId(requestId);
        hookBankInResult.setData(BaseResponse.New(1).toJson());
        if (status.contains("1")) {
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
        HookBankOutResult hookBankOutResult = new HookBankOutResult(Code.ERROR);
        String status = request.getParameter("status");
        String message = request.getParameter("message");
        String amountStr = request.getParameter("real_amount");
        String requestId = request.getParameter("request_id");
        boolean errorResult = StringUtil.handleBlankParams(status, amountStr, requestId, message);
        if (errorResult) {
            return HookBankOutResult.error(BaseResponse.New(4).toJson());
        }
        int amount = 0;
        try {
            amount = Integer.parseInt(amountStr);
        }
        catch (Exception e) {
            e.printStackTrace();
            return HookBankOutResult.error(BaseResponse.New(4).toJson());
        }
        hookBankOutResult.setAmount(amount);
        hookBankOutResult.setRequestId(requestId);
        if (status.contains("1")) {
            hookBankOutResult.setData("Bank Out Success");
            hookBankOutResult.setCode(Code.SUCCESS);
        } else {
            hookBankOutResult.setCode(Code.NOT_SUCCESS);
        }
        return hookBankOutResult;
    }

    @Override
    public HookCardInResult hookCardIn(Param<HttpServletRequest> param) {
        HttpServletRequest request = param.get();
        String status = request.getParameter("status");
        String message = request.getParameter("message");
        String amountStr = request.getParameter("real_amount");
        String requestId = request.getParameter("request_id");
        boolean errorResult = StringUtil.handleBlankParams(status, amountStr, requestId, message);
        if (errorResult) {
            return HookCardInResult.error(BaseResponse.New(4).toJson());
        }
        HookCardInResult hookCardInResult = new HookCardInResult(Code.ERROR);
        int amount = 0;
        try {
            amount = Integer.parseInt(amountStr);
        }
        catch (Exception e) {
            e.printStackTrace();
            return HookCardInResult.error(BaseResponse.New(4).toJson());
        }
        hookCardInResult.setResult_message(message);
        hookCardInResult.setAmount(amount);
        hookCardInResult.setRequestId(requestId);
        if (status.contains("1")) {
            hookCardInResult.setData("Card In Success");
            hookCardInResult.setCode(Code.SUCCESS);
        } else {
            hookCardInResult.setCode(Code.NOT_SUCCESS);
        }
        return hookCardInResult;
    }

    @Override
    public String resultSuccess(Code code, String result) {
        return "{\"code\":" + code.getValue() + ",\"msg\":\"" + result + "\"}";
    }

    @Override
    public String resultError(Code code, String msg) {
        return "{\"code\":" + code.getValue() + ",\"msg\":\"" + msg + "\"}";
    }
}

