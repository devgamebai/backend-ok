/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.vinplay.vbee.common.models.UserModel
 *  javax.servlet.http.HttpServletRequest
 */
package com.payment.service;

import com.payment.core.hook.Param;
import com.payment.model.Result;
import com.payment.provider.Provider;
import com.payment.response.Bank;
import com.payment.response.BankInfo;
import com.payment.response.CardInfo;
import com.vinplay.vbee.common.models.UserModel;
import java.util.List;
import javax.servlet.http.HttpServletRequest;

public interface ProviderService {
    public void add(Provider var1);

    public Result<BankInfo> bankIn(String var1, UserModel var2, String var3, int var4) throws Exception;

    public Result<String> cardIn(String var1, UserModel var2, String var3, String var4, String var5, int var6) throws Exception;

    public Result<String> bankOut(String var1, UserModel var2, String var3, String var4, String var5, String var6) throws Exception;

    public Result<List<Bank>> bankList(String var1) throws Exception;

    public Result<List<CardInfo>> cardList(String var1) throws Exception;

    public Result<String> hookBankIn(String var1, Param<HttpServletRequest> var2);

    public Result<String> hookBankOut(String var1, Param<HttpServletRequest> var2);

    public Result<String> hookCardIn(String var1, Param<HttpServletRequest> var2);

    public Result<List<Bank>> bankListOut(String var1) throws Exception;
}

