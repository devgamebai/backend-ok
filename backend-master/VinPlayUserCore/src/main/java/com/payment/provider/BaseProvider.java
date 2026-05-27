/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.vinplay.vbee.common.models.UserModel
 *  javax.servlet.http.HttpServletRequest
 *  org.json.JSONException
 *  org.json.JSONObject
 */
package com.payment.provider;

import com.payment.core.hook.Param;
import com.payment.model.Code;
import com.payment.provider.Provider;
import com.payment.response.Bank;
import com.payment.response.BankInResult;
import com.payment.response.BankListResult;
import com.payment.response.BankOutResult;
import com.payment.response.CardInResult;
import com.payment.response.CardInfo;
import com.payment.response.CardListResult;
import com.payment.response.HookBankInResult;
import com.payment.response.HookBankOutResult;
import com.payment.response.HookCardInResult;
import com.vinplay.payment.entities.WithDrawPaygateModel;
import com.vinplay.vbee.common.models.UserModel;
import java.util.ArrayList;
import javax.servlet.http.HttpServletRequest;
import org.json.JSONException;
import org.json.JSONObject;

public abstract class BaseProvider
implements Provider {
    @Override
    public BankInResult BankIn(UserModel userInfo, String type, long amount) throws Exception {
        BankInResult result = new BankInResult(Code.ERROR);
        result.setMsg("Not support");
        return result;
    }

    @Override
    public CardInResult CardIn(UserModel userInfo, String code, String serial, String type, int amount) throws Exception {
        CardInResult result = new CardInResult(Code.ERROR);
        result.setMsg("Not support");
        return result;
    }

    @Override
    public BankOutResult BankOut(WithDrawPaygateModel withDrawPaygateModel) throws Exception {
        BankOutResult result = new BankOutResult(Code.ERROR);
        result.setMsg("Not support");
        return result;
    }

    @Override
    public BankListResult BankList() throws Exception {
        BankListResult result = new BankListResult(Code.SUCCESS);
        result.setBanks(new ArrayList<Bank>());
        return result;
    }

    @Override
    public HookBankInResult hookBankIn(Param<HttpServletRequest> param) {
        return HookBankInResult.error("Not support");
    }

    @Override
    public HookBankOutResult hookBankOut(Param<HttpServletRequest> param) {
        return HookBankOutResult.error("Not support");
    }

    @Override
    public HookCardInResult hookCardIn(Param<HttpServletRequest> param) {
        return HookCardInResult.error("Not support");
    }

    @Override
    public CardListResult CardList() throws Exception {
        CardListResult result = new CardListResult(Code.SUCCESS);
        ArrayList<CardInfo> list = new ArrayList<CardInfo>();
        list.add(new CardInfo("Viettel", "Viettel"));
        list.add(new CardInfo("Vinaphone", "Vinaphone"));
        list.add(new CardInfo("Mobifone", "Mobifone"));
        list.add(new CardInfo("Zing", "Zing"));
        list.add(new CardInfo("Vietnamobile", "Vietnamobile"));
        list.add(new CardInfo("Vcoin", "Vcoin"));
        list.add(new CardInfo("Gate", "Gate"));
        list.add(new CardInfo("Garena", "Garena"));
        result.setCards(list);
        return result;
    }

    @Override
    public String resultSuccess(Code code, String result) {
        JSONObject jsonObject = new JSONObject();
        try {
            jsonObject.put("success", code);
            jsonObject.put("data", result);
        }
        catch (JSONException e) {
            e.printStackTrace();
        }
        return jsonObject.toString();
    }

    @Override
    public String resultError(Code code, String msg) {
        JSONObject jsonObject = new JSONObject();
        try {
            jsonObject.put("success", code);
            jsonObject.put("msg", msg);
        }
        catch (JSONException e) {
            e.printStackTrace();
        }
        return jsonObject.toString();
    }

    @Override
    public BankListResult BankListOut() throws Exception {
        BankListResult result = new BankListResult(Code.SUCCESS);
        result.setBanks(new ArrayList<Bank>());
        return result;
    }
}

