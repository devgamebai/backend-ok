/*
 * Decompiled with CFR 0.144.
 */
package com.vinplay.vbee.common.response;

import com.vinplay.vbee.common.response.BaseResponseModel;

public class GiftCodeUpdateResponse
extends BaseResponseModel {
    public long moneyGiftCodeVin;
    public long moneyGiftCodeXu;
    public long currentMoneyVin;
    public long currentMoneyXu;
    public int moneyType;
    public String giftCode;
    public String type;
    public String agent;
    public int use;
    public String source;

    public GiftCodeUpdateResponse(boolean success, String errorCode) {
        super(success, errorCode);
    }
}

