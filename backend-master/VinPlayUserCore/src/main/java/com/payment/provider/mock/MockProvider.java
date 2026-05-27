/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.vinplay.vbee.common.models.UserModel
 *  javax.servlet.http.HttpServletRequest
 *  org.apache.http.HttpEntity
 *  org.apache.http.client.entity.UrlEncodedFormEntity
 *  org.apache.http.client.methods.CloseableHttpResponse
 *  org.apache.http.client.methods.HttpPost
 *  org.apache.http.client.methods.HttpUriRequest
 *  org.apache.http.client.utils.URIBuilder
 *  org.apache.http.impl.client.CloseableHttpClient
 *  org.apache.http.impl.client.HttpClients
 *  org.apache.http.message.BasicNameValuePair
 *  org.json.JSONObject
 */
package com.payment.provider.mock;

import com.payment.core.common.ProviderUtil;
import com.payment.core.common.StringUtil;
import com.payment.core.hook.Param;
import com.payment.entities.HistoryApplyForEntity;
import com.payment.entities.HistoryBankEntity;
import com.payment.entities.TopUpEntity;
import com.payment.model.Code;
import com.payment.provider.BaseProvider;
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
import java.util.Arrays;
import javax.servlet.http.HttpServletRequest;
import org.apache.http.HttpEntity;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.message.BasicNameValuePair;
import org.json.JSONObject;

public class MockProvider
extends BaseProvider {
    @Override
    public BankInResult BankIn(UserModel userInfo, String type, long amount) throws Exception {
        System.out.println("BankIn " + userInfo.getId() + " " + type + " " + amount);
        BankInResult result = new BankInResult(Code.ERROR);
        String requestId = StringUtil.timestampToText() + StringUtil.digitalText(5);
        HistoryBankEntity historyBank = new HistoryBankEntity(requestId);
        historyBank.setFid(String.valueOf(userInfo.getId()));
        historyBank.setNick_name(userInfo.getNickname());
        historyBank.setCash((int)amount);
        historyBank.setType(type);
        result.setHistoryBank(historyBank);
        BankInfo bankInfo = new BankInfo();
        bankInfo.setBankAccount("Nguyen Van A");
        bankInfo.setBankNo("123456789");
        bankInfo.setAmount((int)amount);
        bankInfo.setBankType("Vietcombank");
        bankInfo.setQr("qr");
        bankInfo.setQrUrl("qrUrl");
        bankInfo.setNote("note");
        result.setBankInfo(bankInfo);
        result.setCode(Code.SUCCESS);
        new Thread(() -> {
            try {
                Thread.sleep(10000L);
                CloseableHttpClient httpclient = HttpClients.createDefault();
                URIBuilder url = new URIBuilder("http://localhost:29500");
                url.setPath("/hook/default/callback/bank");
                HttpPost request = new HttpPost(url.build());
                ArrayList<BasicNameValuePair> formParams = new ArrayList<BasicNameValuePair>();
                formParams.add(new BasicNameValuePair("request_id", requestId));
                formParams.add(new BasicNameValuePair("amount", String.valueOf(amount)));
                request.setEntity((HttpEntity)new UrlEncodedFormEntity(formParams));
                CloseableHttpResponse response = httpclient.execute((HttpUriRequest)request);
                JSONObject obj = ProviderUtil.getResponseAll(response);
                if (obj != null) {
                    if (obj.getInt("code") == 0) {
                        System.out.println("Success");
                    } else {
                        System.out.println("Error");
                    }
                }
            }
            catch (Exception e) {
                throw new RuntimeException(e);
            }
        }).start();
        return result;
    }

    private boolean isValidType(String type) {
        return Arrays.asList("Viettel", "Vinaphone", "Mobifone", "Zing", "Vietnamobile", "Vcoin", "Gate", "Garena").contains(type);
    }

    @Override
    public CardInResult CardIn(UserModel userInfo, String code, String serial, String type, int amount) throws Exception {
        String requestId = StringUtil.timestampToText() + StringUtil.digitalText(5);
        CardInResult cardInResult = new CardInResult(Code.ERROR);
        if (!this.isValidType(type)) {
            cardInResult.setCode(Code.NOT_SUCCESS);
            cardInResult.setMsg("Invalid card type");
            return cardInResult;
        }
        TopUpEntity topUpEntity = new TopUpEntity();
        topUpEntity.setCash(amount);
        topUpEntity.setRequest_id(requestId);
        topUpEntity.setSerial(serial);
        topUpEntity.setCode(code);
        topUpEntity.setType(type);
        cardInResult.setTopUpEntity(topUpEntity);
        cardInResult.setCode(Code.SUCCESS);
        new Thread(() -> {
            try {
                Thread.sleep(10000L);
                CloseableHttpClient httpclient = HttpClients.createDefault();
                URIBuilder url = new URIBuilder("http://localhost:29500");
                url.setPath("/hook/default/callback/card");
                HttpPost request = new HttpPost(url.build());
                ArrayList<BasicNameValuePair> formParams = new ArrayList<BasicNameValuePair>();
                formParams.add(new BasicNameValuePair("request_id", requestId));
                formParams.add(new BasicNameValuePair("amount", String.valueOf(amount)));
                request.setEntity((HttpEntity)new UrlEncodedFormEntity(formParams));
                CloseableHttpResponse response = httpclient.execute((HttpUriRequest)request);
                JSONObject obj = ProviderUtil.getResponseAll(response);
                if (obj != null) {
                    if (obj.getInt("code") == 0) {
                        System.out.println("Success");
                    } else {
                        System.out.println("Error");
                    }
                }
            }
            catch (Exception e) {
                throw new RuntimeException(e);
            }
        }).start();
        return cardInResult;
    }

    @Override
    public BankOutResult BankOut(WithDrawPaygateModel withDrawPaygateModel) throws Exception {
        BankOutResult result = new BankOutResult(Code.SUCCESS);
        new Thread(() -> {
            try {
                Thread.sleep(10000L);
                CloseableHttpClient httpclient = HttpClients.createDefault();
                URIBuilder url = new URIBuilder("http://localhost:29500");
                url.setPath("/hook/default/callback/bankOut");
                HttpPost request = new HttpPost(url.build());
                ArrayList<BasicNameValuePair> formParams = new ArrayList<BasicNameValuePair>();
                formParams.add(new BasicNameValuePair("request_id", withDrawPaygateModel.Id));
                formParams.add(new BasicNameValuePair("amount", String.valueOf(withDrawPaygateModel.Amount)));
                request.setEntity((HttpEntity)new UrlEncodedFormEntity(formParams));
                CloseableHttpResponse response = httpclient.execute((HttpUriRequest)request);
                JSONObject obj = ProviderUtil.getResponseAll(response);
                if (obj != null) {
                    if (obj.getInt("code") == 0) {
                        System.out.println("Success");
                    } else {
                        System.out.println("Error");
                    }
                }
            }
            catch (Exception e) {
                throw new RuntimeException(e);
            }
        }).start();
        HistoryApplyForEntity historyBank = new HistoryApplyForEntity(withDrawPaygateModel.Id);
        historyBank.setNickName(withDrawPaygateModel.Nickname);
        historyBank.setCash(withDrawPaygateModel.Amount);
        historyBank.setCashReal(withDrawPaygateModel.Amount);
        historyBank.setType(withDrawPaygateModel.BankCode);
        historyBank.setText(withDrawPaygateModel.BankAccountName);
        result.setHistoryApplyFor(historyBank);
        return result;
    }

    @Override
    public BankListResult BankList() throws Exception {
        BankListResult bankListResult = new BankListResult(Code.SUCCESS);
        ArrayList<Bank> banks = new ArrayList<Bank>();
        banks.add(new Bank("ACB BANK", "ACB"));
        banks.add(new Bank("BIDV BANK", "BIDV"));
        bankListResult.setBanks(banks);
        return bankListResult;
    }

    @Override
    public HookBankInResult hookBankIn(Param<HttpServletRequest> param) {
        System.out.println("Mock hookBankIn ");
        HookBankInResult result = new HookBankInResult(Code.SUCCESS);
        HttpServletRequest request = param.get();
        String requestId = request.getParameter("request_id");
        String amountStr = request.getParameter("amount");
        int amount = 0;
        try {
            amount = Integer.parseInt(amountStr);
        }
        catch (Exception e) {
            e.printStackTrace();
        }
        result.setRequestId(requestId);
        result.setAmount(amount);
        result.setData("Success");
        return result;
    }

    @Override
    public HookBankOutResult hookBankOut(Param<HttpServletRequest> param) {
        System.out.println("Mock hookBankIn ");
        HookBankOutResult result = new HookBankOutResult(Code.SUCCESS);
        HttpServletRequest request = param.get();
        String requestId = request.getParameter("request_id");
        String amountStr = request.getParameter("amount");
        int amount = 0;
        try {
            amount = Integer.parseInt(amountStr);
        }
        catch (Exception e) {
            e.printStackTrace();
        }
        result.setRequestId(requestId);
        result.setAmount(amount);
        result.setRollback(false);
        result.setData("Success");
        return result;
    }

    @Override
    public HookCardInResult hookCardIn(Param<HttpServletRequest> param) {
        System.out.println("Mock hookCardIn ");
        HookCardInResult result = new HookCardInResult(Code.SUCCESS);
        HttpServletRequest request = param.get();
        String requestId = request.getParameter("request_id");
        String amountStr = request.getParameter("amount");
        int amount = 0;
        try {
            amount = Integer.parseInt(amountStr);
        }
        catch (Exception e) {
            e.printStackTrace();
        }
        result.setRequestId(requestId);
        result.setAmount(amount);
        result.setData("Success");
        return result;
    }

    @Override
    public String name() {
        return "mock";
    }
}

