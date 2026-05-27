/*
 * Decompiled with CFR 0.152.
 */
package com.payment.response;

import com.payment.model.Code;
import com.payment.response.CardInfo;
import java.util.List;

public class CardListResult {
    private List<CardInfo> cards;
    private Code code;
    private String msg;

    public CardListResult(Code code) {
        this.code = code;
    }

    public Code getCode() {
        return this.code;
    }

    public void setCode(Code code) {
        this.code = code;
    }

    public String getMsg() {
        return this.msg;
    }

    public void setMsg(String msg) {
        this.msg = msg;
    }

    public List<CardInfo> getCards() {
        return this.cards;
    }

    public void setCards(List<CardInfo> cards) {
        this.cards = cards;
    }
}

