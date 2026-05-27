/*
 * Decompiled with CFR 0.144.
 */
package com.vinplay.vbee.common.response;

import com.vinplay.vbee.common.response.BaseResponseModel;

public class GiftCodeRestoreResponse
extends BaseResponseModel {
    public String giftCode;

    public String getGiftCode() {
        return this.giftCode;
    }

    public void setGiftCode(String giftCode) {
        this.giftCode = giftCode;
    }

    public GiftCodeRestoreResponse(boolean success, String errorCode) {
        super(success, errorCode);
    }
}

