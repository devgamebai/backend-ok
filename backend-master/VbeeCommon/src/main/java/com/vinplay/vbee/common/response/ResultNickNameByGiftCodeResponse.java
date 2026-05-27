/*
 * Decompiled with CFR 0.144.
 */
package com.vinplay.vbee.common.response;

import com.vinplay.vbee.common.response.BaseResponseModel;
import com.vinplay.vbee.common.response.GiftCodeByNickNameResponse;
import java.util.ArrayList;
import java.util.List;

public class ResultNickNameByGiftCodeResponse
extends BaseResponseModel {
    private long total;
    private String giftCodeNotExits;
    private String giftCodeNotUse;
    private List<GiftCodeByNickNameResponse> transactions = new ArrayList<GiftCodeByNickNameResponse>();

    public String getGiftCodeNotUse() {
        return this.giftCodeNotUse;
    }

    public void setGiftCodeNotUse(String giftCodeNotUse) {
        this.giftCodeNotUse = giftCodeNotUse;
    }

    public String getGiftCodeNotExits() {
        return this.giftCodeNotExits;
    }

    public void setGiftCodeNotExits(String giftCodeNotExits) {
        this.giftCodeNotExits = giftCodeNotExits;
    }

    public ResultNickNameByGiftCodeResponse(boolean success, String errorCode) {
        super(success, errorCode);
    }

    public List<GiftCodeByNickNameResponse> getTransactions() {
        return this.transactions;
    }

    public void setTransactions(List<GiftCodeByNickNameResponse> transactions) {
        this.transactions = transactions;
    }

    public long getTotal() {
        return this.total;
    }

    public void setTotal(long total) {
        this.total = total;
    }
}

