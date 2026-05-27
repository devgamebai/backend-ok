/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.vinplay.vbee.common.models.UserModel
 *  javax.servlet.http.HttpServletRequest
 */
package com.payment.provider;

import com.payment.core.hook.Param;
import com.payment.model.Code;
import com.payment.response.BankInResult;
import com.payment.response.BankListResult;
import com.payment.response.BankOutResult;
import com.payment.response.CardInResult;
import com.payment.response.CardListResult;
import com.payment.response.HookBankInResult;
import com.payment.response.HookBankOutResult;
import com.payment.response.HookCardInResult;
import com.vinplay.payment.entities.WithDrawPaygateModel;
import com.vinplay.vbee.common.models.UserModel;
import javax.servlet.http.HttpServletRequest;

public interface Provider {
    public BankInResult BankIn(UserModel var1, String var2, long var3) throws Exception;

    public CardInResult CardIn(UserModel var1, String var2, String var3, String var4, int var5) throws Exception;

    public BankOutResult BankOut(WithDrawPaygateModel var1) throws Exception;

    public BankListResult BankList() throws Exception;

    public CardListResult CardList() throws Exception;

    public HookBankInResult hookBankIn(Param<HttpServletRequest> var1);

    public HookBankOutResult hookBankOut(Param<HttpServletRequest> var1);

    public HookCardInResult hookCardIn(Param<HttpServletRequest> var1);

    public String name();

    public String resultSuccess(Code var1, String var2);

    public String resultError(Code var1, String var2);

    public BankListResult BankListOut() throws Exception;
}

