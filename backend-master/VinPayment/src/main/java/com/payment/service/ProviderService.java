package com.payment.service;

import com.payment.core.hook.Param;
import com.payment.provider.Provider;
import com.payment.model.Result;
import com.payment.response.Bank;
import com.payment.response.BankInfo;
import com.payment.response.CardInfo;
import com.vinplay.vbee.common.models.UserModel;

import javax.servlet.http.HttpServletRequest;
import java.util.List;

public interface ProviderService {
    void add(Provider provider);

    Result<BankInfo> bankIn(String providerName, UserModel userInfo, String type, int amount) throws Exception;

    Result<String> cardIn(String providerName, UserModel userInfo, String code, String serial, String type, int amount) throws Exception;

    Result<String> bankOut(String providerName,  UserModel userInfo,String requestId, String nickName, String admin, String ip) throws Exception;

    Result<List<Bank>> bankList(String providerName) throws Exception;
    Result<List<CardInfo>> cardList(String providerName) throws Exception;

    Result<String> hookBankIn(String providerName, Param<HttpServletRequest> param);

    Result<String> hookBankOut(String providerName, Param<HttpServletRequest> param);

    Result<String> hookCardIn(String providerName, Param<HttpServletRequest> param);

    Result<List<Bank>> bankListOut(String providerName) throws Exception;
}
